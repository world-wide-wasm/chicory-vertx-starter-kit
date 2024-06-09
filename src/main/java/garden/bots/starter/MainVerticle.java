package garden.bots.starter;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.runtime.*;
import com.dylibso.chicory.runtime.Module;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;
import com.dylibso.chicory.wasm.types.ValueType;
import com.dylibso.chicory.wasm.types.Value;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public class MainVerticle extends AbstractVerticle {
  private ReentrantLock lock = new ReentrantLock();

  // Route Handler
  private Handler<RoutingContext> defineHandler(Module module, String wasmFunctionName) {

    //! Instantiate the wasm plugin
    System.out.println("🚀 instantiate the wasm plugin");

    Instance instance = module.instantiate();

    // automatically exported by TinyGo
    ExportFunction malloc = instance.export("malloc");
    ExportFunction free = instance.export("free");

    ExportFunction pluginFunc = instance.export(wasmFunctionName);
    Memory memory = instance.memory();

    return ctx -> {
      ctx.request().body().andThen(asyncRes -> {
        // Get the HTTP request parameter
        var requestParameter = asyncRes.result().toString();

        lock.lock();
        try {

          int len = requestParameter.getBytes().length;
          // allocate {len} bytes of memory, this returns a pointer to that memory
          int ptr = malloc.apply(Value.i32(len))[0].asInt();
          // We can now write the message to the module's memory:
          memory.writeString(ptr, requestParameter);

          Value result = pluginFunc.apply(Value.i32(ptr), Value.i32(len))[0];
          free.apply(Value.i32(ptr), Value.i32(len));

          int valuePosition = (int) ((result.asLong() >>> 32) & 0xFFFFFFFFL);
          int valueSize = (int) (result.asLong() & 0xFFFFFFFFL);

          byte[] bytes = memory.readBytes(valuePosition, valueSize);
          String strResult = new String(bytes, StandardCharsets.UTF_8);

          // Send the response to the HTTP client
          ctx.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(strResult);

        } catch (Exception e) {
          ctx.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(e.getMessage());
        } finally {
          lock.unlock();
        }

      });
    };
  }


  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // Create a router and a POST route
    var router = Router.router(vertx);
    var route = router.route().method(HttpMethod.POST);

    // Check environment variables
    var wasmFileLocalLocation = Optional.ofNullable(System.getenv("WASM_FILE")).orElse("./demo-plugin/demo.wasm");
    var wasmFunctionName = Optional.ofNullable(System.getenv("FUNCTION_NAME")).orElse("hello");

    if(wasmFunctionName.isBlank()) {
      startPromise.fail(new Throwable("FAIL: Fill FUNCTION_NAME"));
    }

    if (wasmFileLocalLocation.isBlank()) {
      startPromise.fail(new Throwable("FAIL: Fill WASM_FILE"));
    }

    // Create fd_write method to avoid this warning message:
    // "WARNING: Could not find host function for import number: 0 named wasi_snapshot_preview1.fd_write"

    /*
    var fd_write = new HostFunction(
      (Instance instance, Value... params) -> {
        return null;
      },
      "wasi_snapshot_preview1",
      "fd_write",
      List.of(ValueType.I32, ValueType.I32),
      List.of());

    var imports = new HostImports(new HostFunction[] {fd_write});

     */


    //https://github.com/dylibso/chicory/tree/main/wasi#how-to-use
    var logger = new SystemLogger();
    // let's just use the default options for now
    var options = WasiOptions.builder().build();
    // create our instance of wasip1
    var wasi = new WasiPreview1(logger, WasiOptions.builder().build());
    // turn those into host imports. Here we could add any other custom imports we have
    var imports = new HostImports(wasi.toHostFunctions());

    // create the module
    var module = Module.builder(new File(wasmFileLocalLocation)).build().withHostImports(imports);
    // instantiate and connect our imports, this will execute the module
    //var instance = module.instantiate();

    // Define the route handler
    var handler = this.defineHandler(module, wasmFunctionName);
    // This handler will be called for any POST request
    route.handler(handler);

    var httpPort = Optional.ofNullable(System.getenv("HTTP_PORT")).orElse("8080");
    // Create an HTTP server
    var server = vertx.createHttpServer();

    //! Start the HTTP server
    server.requestHandler(router).listen(Integer.parseInt(httpPort), http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("Server started on port " + httpPort);
      } else {
        startPromise.fail(http.cause());
      }
    });

  }
}
