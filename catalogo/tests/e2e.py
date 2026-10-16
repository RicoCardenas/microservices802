"""Prueba aislada: crea PostgreSQL temporal y ejecuta los dos JAR ya compilados."""
import base64
import concurrent.futures
import glob
import http.server
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request
import uuid

ROOT = Path(__file__).resolve().parents[2]
LOGS = ROOT / "catalogo/target/e2e"
KEY = uuid.uuid4().hex
PASSWORD = "Clave123!"
PG_BIN = Path(os.environ.get("PG_BIN") or sorted(glob.glob("/usr/lib/postgresql/*/bin"), key=lambda p: int(Path(p).parent.name))[-1])
JAVA = str(Path(os.environ["JAVA_HOME"]) / "bin/java") if os.environ.get("JAVA_HOME") else shutil.which("java")
checks = []


def port():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def check(name, condition):
    assert condition, name
    checks.append(name)
    print("OK: " + name, flush=True)


def api(base, method, path, body=None, user=None, headers=None):
    request_headers = {"Content-Type": "application/json", "X-Internal-Token": KEY}
    if user:
        request_headers["Authorization"] = "Basic " + base64.b64encode((user["mail"] + ":" + PASSWORD).encode()).decode()
    request_headers.update(headers or {})
    request = urllib.request.Request(base + path, data=None if body is None else json.dumps(body).encode(), headers=request_headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    data = response.read()
    return response.status, json.loads(data) if data else None


def wait_until(condition, seconds=30):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline:
        try:
            if condition():
                return
        except (OSError, urllib.error.URLError):
            pass
        time.sleep(0.2)
    raise AssertionError("Se agotó el tiempo de espera")


class FaultProxy(http.server.BaseHTTPRequestHandler):
    target = ""
    lose_charge_response = False
    reject_refund = False
    malformed_charge_response = False

    def log_message(self, *args):
        pass

    def do_POST(self):
        if self.headers.get("Transfer-Encoding", "").lower() == "chunked":
            chunks = []
            while True:
                size = int(self.rfile.readline().split(b";", 1)[0], 16)
                if size == 0:
                    while self.rfile.readline() not in (b"\r\n", b"\n", b""):
                        pass
                    break
                chunks.append(self.rfile.read(size))
                self.rfile.read(2)
            body = b"".join(chunks)
        else:
            body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
        if self.path.endswith("/saldo/cancelar") and self.reject_refund:
            self.send_error(503)
            return
        request = urllib.request.Request(self.target + self.path, data=body, method="POST", headers={
            "Content-Type": "application/json", "X-Internal-Token": self.headers.get("X-Internal-Token", "")})
        try:
            upstream = urllib.request.urlopen(request, timeout=10)
        except urllib.error.HTTPError as error:
            upstream = error
        data = upstream.read()
        if self.path.endswith("/saldo/descontar") and self.malformed_charge_response:
            data = b"{}"
        if self.path.endswith("/saldo/descontar") and self.lose_charge_response:
            self.connection.shutdown(socket.SHUT_RDWR)
            self.connection.close()
            return
        self.send_response(upstream.status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def run():
    LOGS.mkdir(parents=True, exist_ok=True)
    processes, handles = [], []
    pg_started = False
    proxy = None
    with tempfile.TemporaryDirectory(prefix="catalogo-e2e-") as temporary:
        cluster = Path(temporary) / "pg"
        sockets = Path(temporary) / "sockets"
        sockets.mkdir()
        pg_port, auth_port, catalog_port = port(), port(), port()
        auth_url, catalog_url = f"http://127.0.0.1:{auth_port}", f"http://127.0.0.1:{catalog_port}"
        pg_args = ["-h", "127.0.0.1", "-p", str(pg_port), "-U", "catalogo_test", "-d", "postgres", "-v", "ON_ERROR_STOP=1"]

        def sql(statement, fail=False):
            result = subprocess.run([str(PG_BIN / "psql"), *pg_args, "-At", "-c", statement], capture_output=True, text=True)
            if fail:
                return result.returncode != 0
            assert result.returncode == 0, result.stderr
            return result.stdout.strip()

        def stop_services():
            for process in reversed(processes):
                process.terminate()
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    process.kill()
                    process.wait()
            processes.clear()

        def start(module, server_port, extra=()):
            log = (LOGS / f"{module}-{len(handles)}.log").open("w")
            handles.append(log)
            args = [JAVA, "-jar", str(ROOT / module / "target" / (module + "-0.0.1-SNAPSHOT.jar")),
                    f"--server.port={server_port}", f"--spring.datasource.url=jdbc:postgresql://127.0.0.1:{pg_port}/postgres",
                    "--spring.datasource.username=catalogo_test", "--spring.datasource.password=", "--debug=false", f"--auth.internal-token={KEY}", *extra]
            process = subprocess.Popen(args, stdout=log, stderr=subprocess.STDOUT)
            processes.append(process)

        def register(label):
            payload = {"name": label, "mail": label + "@a.co", "password": PASSWORD, "rol": "USER", "age": 25, "phoneNumber": "3001234567", "saldo": 99999}
            status, result = api(auth_url, "POST", "/api/ecommerce/user/save", payload)
            assert status == 200 and result["success"], result
            user = result["user"]
            assert user["saldo"] == 0
            return user

        def saldo(user):
            return api(auth_url, "GET", "/api/ecommerce/user/search/" + user["idUser"])[1]["user"]["saldo"]

        def fund(user, amount=100):
            return api(auth_url, "POST", f"/api/ecommerce/user/{user['idUser']}/saldo/recargar", {"idOperacion": str(uuid.uuid4()), "monto": amount})

        def product(stock, price=12.50):
            return api(catalog_url, "POST", "/api/ecommerce/producto/save", {"nombre": "Producto de prueba", "descripcion": "E2E", "precio": price, "stock": stock})[1]["data"]

        def cart(user, action="", body=None, method="POST"):
            return api(catalog_url, method, "/api/ecommerce/carrito/" + user["idUser"] + action, body, user)

        try:
            subprocess.run([str(PG_BIN / "initdb"), "-D", str(cluster), "-A", "trust", "--no-locale", "-U", "catalogo_test"], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True)
            subprocess.run([str(PG_BIN / "pg_ctl"), "-D", str(cluster), "-l", str(LOGS / "postgres.log"), "-o", f"-h 127.0.0.1 -p {pg_port} -k {sockets}", "start"], stdout=subprocess.DEVNULL, check=True)
            pg_started = True
            start("auth", auth_port)
            wait_until(lambda: api(auth_url, "GET", "/api/ecommerce/user/search/none")[0] == 200)
            check("Auth crea sus tablas en auth sin duplicarlas en public", sql("""
                select to_regclass('auth.users') is not null
                    and to_regclass('auth.movimientos_saldo') is not null
                    and to_regclass('public.users') is null
                    and to_regclass('public.movimientos_saldo') is null
                """) == "t")
            FaultProxy.target = auth_url
            proxy = http.server.ThreadingHTTPServer(("127.0.0.1", port()), FaultProxy)
            threading.Thread(target=proxy.serve_forever, daemon=True).start()
            start("catalogo", catalog_port, [f"--auth.base-url=http://127.0.0.1:{proxy.server_port}"])
            wait_until(lambda: api(catalog_url, "GET", "/api/ecommerce/producto/list")[0] == 200)
            check("carritos y compras referencian auth.users", sql("""
                select count(*) from pg_constraint where contype='f'
                    and conrelid in ('catalogo.carritos'::regclass, 'catalogo.compras'::regclass)
                    and confrelid='auth.users'::regclass
                """) == "2")
            alice, bob = register("alice"), register("bob")
            check("registro ignora cualquier saldo enviado por el cliente", saldo(alice) == 0)
            fund(alice)
            fund(bob)
            charge_ids = [str(uuid.uuid4()), str(uuid.uuid4())]
            with concurrent.futures.ThreadPoolExecutor(2) as executor:
                futures = [executor.submit(api, auth_url, "POST", f"/api/ecommerce/user/{alice['idUser']}/saldo/descontar",
                                           {"idOperacion": operation, "monto": 80}) for operation in charge_ids]
                charges = [future.result()[1] for future in futures]
            check("cobros concurrentes del mismo usuario no sobregiran saldo", sum(c["success"] for c in charges) == 1 and saldo(alice) == 20)
            approved_id = charge_ids[next(i for i, charge in enumerate(charges) if charge["success"])]
            api(auth_url, "POST", f"/api/ecommerce/user/{alice['idUser']}/saldo/cancelar", {"idOperacion": approved_id, "monto": 80})
            check("devolución conserva saldo tras cobros concurrentes", saldo(alice) == 100)
            check("recarga pertenece a Auth y requiere clave interna", api(auth_url, "POST", f"/api/ecommerce/user/{alice['idUser']}/saldo/recargar", {"idOperacion": "wrong-key", "monto": 1}, headers={"X-Internal-Token": "wrong"})[0] == 401)
            p = product(5)
            check("producto requiere clave administrativa", api(catalog_url, "DELETE", "/api/ecommerce/producto/delete/" + p["idProducto"], headers={"X-Internal-Token": "wrong"})[0] == 401)
            check("carrito requiere credenciales", api(catalog_url, "GET", "/api/ecommerce/carrito/" + alice["idUser"])[0] == 401)
            check("usuario no puede operar carrito ajeno", api(catalog_url, "POST", "/api/ecommerce/carrito/" + bob["idUser"] + "/agregar", {"idProducto": p["idProducto"], "cantidad": 1}, alice)[0] == 403)
            _, added = cart(alice, "/agregar", {"idProducto": p["idProducto"], "cantidad": 2})
            check("total monetario del carrito", added["success"] and added["data"]["total"] == 25)
            check("no agrega cantidad acumulada mayor que stock", not cart(alice, "/agregar", {"idProducto": p["idProducto"], "cantidad": 4})[1]["success"])
            check("datos inválidos devuelven 400", cart(alice, "/agregar", {"idProducto": p["idProducto"]})[0] == 400)
            compra = str(uuid.uuid4())
            check("compra completa por HTTP Auth", cart(alice, "/comprar", {"idOperacion": compra})[1]["success"])
            check("compra descontó saldo y stock y desactivó carrito", saldo(alice) == 75 and sql(f"select stock from catalogo.productos where id_producto='{p['idProducto']}'") == "3" and cart(alice, method="GET")[1]["data"]["estado"] == "Desactivado")
            check("repetir compra no duplica cobro", cart(alice, "/comprar", {"idOperacion": compra})[1]["success"] and saldo(alice) == 75)
            check("producto no permite hard delete SQL", sql(f"delete from catalogo.productos where id_producto='{p['idProducto']}'", fail=True))
            check("carrito no permite hard delete SQL", sql(f"delete from catalogo.carritos where id_usuario='{alice['idUser']}'", fail=True))
            check("no permite carrito huérfano", sql("insert into catalogo.carritos(id_carrito,id_usuario) values ('orphan','unknown')", fail=True))
            check("relación usuario carrito es 1 a 1", sql(f"insert into catalogo.carritos(id_carrito,id_usuario) values ('duplicate','{alice['idUser']}')", fail=True))
            scarce = product(1, 10)
            cart(alice, "/agregar", {"idProducto": scarce["idProducto"], "cantidad": 1})
            cart(bob, "/agregar", {"idProducto": scarce["idProducto"], "cantidad": 1})
            with concurrent.futures.ThreadPoolExecutor(2) as executor:
                futures = [executor.submit(cart, user, "/comprar", {"idOperacion": str(uuid.uuid4())}) for user in (alice, bob)]
                outcomes = [future.result()[1]["success"] for future in futures]
            check("dos compradores concurrentes no sobrevenden última unidad", sum(outcomes) == 1 and sql(f"select stock from catalogo.productos where id_producto='{scarce['idProducto']}'") == "0")
            charlie = register("charlie")
            fund(charlie)
            refund_product = product(2, 10)
            cart(charlie, "/agregar", {"idProducto": refund_product["idProducto"], "cantidad": 1})
            failed = str(uuid.uuid4())
            FaultProxy.lose_charge_response = True
            FaultProxy.reject_refund = True
            _, rejected = cart(charlie, "/comprar", {"idOperacion": failed})
            check("respuesta perdida después de cobrar rechaza compra", not rejected["success"] and rejected["data"]["estado"] == "CANCELACION_PENDIENTE")
            check("fallo restaura stock y mantiene carrito activo", saldo(charlie) == 90 and sql(f"select stock from catalogo.productos where id_producto='{refund_product['idProducto']}'") == "2" and cart(charlie, method="GET")[1]["data"]["estado"] == "Activo")
            FaultProxy.lose_charge_response = False
            FaultProxy.reject_refund = False
            wait_until(lambda: saldo(charlie) == 100, 25)
            check("recuperación automática devuelve cobro confirmado", sql(f"select estado from catalogo.compras where id_compra='{failed}'") == "CANCELADA")
            late = api(auth_url, "POST", f"/api/ecommerce/user/{charlie['idUser']}/saldo/descontar", {"idOperacion": failed, "monto": 10})[1]
            check("cancelación bloquea cobro tardío con misma operación", not late["success"] and saldo(charlie) == 100)
            FaultProxy.malformed_charge_response = True
            invalid_response = str(uuid.uuid4())
            status, malformed = cart(charlie, "/comprar", {"idOperacion": invalid_response})
            FaultProxy.malformed_charge_response = False
            check("respuesta incompleta de Auth rechaza y devuelve cobro", status == 200 and not malformed["success"]
                  and malformed["data"]["estado"] == "CANCELADA" and saldo(charlie) == 100)
            interrupted = str(uuid.uuid4())
            cart_id = cart(charlie, method="GET")[1]["data"]["idCarrito"]
            sql(f"""BEGIN;
                update catalogo.productos set stock=stock-1 where id_producto='{refund_product['idProducto']}';
                update catalogo.carritos set compra_pendiente='{interrupted}' where id_carrito='{cart_id}';
                insert into catalogo.compras(id_compra,id_carrito,id_usuario,total,estado,creada_en)
                values ('{interrupted}','{cart_id}','{charlie['idUser']}',1000,'PENDIENTE',current_timestamp-interval '120 seconds'); COMMIT;""")
            api(auth_url, "POST", f"/api/ecommerce/user/{charlie['idUser']}/saldo/descontar", {"idOperacion": interrupted, "monto": 10})
            wait_until(lambda: sql(f"select estado from catalogo.compras where id_compra='{interrupted}'") == "CANCELADA", 25)
            check("recupera compra interrumpida persistida", saldo(charlie) == 100 and sql(f"select stock from catalogo.productos where id_producto='{refund_product['idProducto']}'") == "2")
            check("DELETE producto es desactivación lógica", api(catalog_url, "DELETE", "/api/ecommerce/producto/delete/" + p["idProducto"])[1]["success"] and sql(f"select count(*) from catalogo.productos where id_producto='{p['idProducto']}' and not activo") == "1")
            stop_services()
            tables = ("auth.users", "auth.movimientos_saldo", "catalogo.catalogos", "catalogo.productos",
                      "catalogo.carritos", "catalogo.carrito_productos", "catalogo.compras")
            snapshots = {table: (sql(f"select '{table}'::regclass::oid"),
                                 sql(f"select row_to_json(t)::text from {table} t order by 1"))
                         for table in tables}
            start("auth", auth_port)
            wait_until(lambda: api(auth_url, "GET", "/api/ecommerce/user/search/none")[0] == 200)
            start("catalogo", catalog_port, [f"--auth.base-url=http://127.0.0.1:{proxy.server_port}"])
            wait_until(lambda: api(catalog_url, "GET", "/api/ecommerce/producto/list")[0] == 200)
            check("reinicio conserva tablas y registros de ambos esquemas", all(
                (sql(f"select '{table}'::regclass::oid"),
                 sql(f"select row_to_json(t)::text from {table} t order by 1")) == snapshot
                for table, snapshot in snapshots.items()))
            check("reinicio conserva autenticación y acceso al carrito", cart(charlie, method="GET")[0] == 200
                  and saldo(charlie) == 100)
            (LOGS / "resultado.json").write_text(json.dumps({"checks": checks, "total": len(checks)}, indent=2, ensure_ascii=False) + "\n")
            print(f"{len(checks)} verificaciones E2E aprobadas", flush=True)
        finally:
            stop_services()
            if proxy:
                proxy.shutdown()
                proxy.server_close()
            if pg_started:
                subprocess.run([str(PG_BIN / "pg_ctl"), "-D", str(cluster), "-m", "immediate", "stop"], stdout=subprocess.DEVNULL, check=True)
            for handle in handles:
                handle.close()


if __name__ == "__main__":
    run()
