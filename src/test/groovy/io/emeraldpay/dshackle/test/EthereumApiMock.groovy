package io.emeraldpay.dshackle.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.emeraldpay.dshackle.upstream.EthereumApi
import io.emeraldpay.grpc.Chain
import io.infinitape.etherjar.rpc.RpcClient
import io.infinitape.etherjar.rpc.RpcResponseError
import io.infinitape.etherjar.rpc.json.ResponseJson
import org.jetbrains.annotations.NotNull
import reactor.core.publisher.Mono

class EthereumApiMock extends EthereumApi {

    List<PredefinedResponse> predefined = []
    private ObjectMapper objectMapper

    EthereumApiMock(@NotNull RpcClient rpcClient, @NotNull ObjectMapper objectMapper, @NotNull Chain chain) {
        super(rpcClient, objectMapper, chain)
        this.objectMapper = objectMapper
    }

    EthereumApiMock answer(@NotNull String method, List<Object> params, Object result) {
        predefined << new PredefinedResponse(method: method, params: params, result: result)
        return this
    }

    @Override
    Mono<byte[]> execute(int id, @NotNull String method, @NotNull List<?> params) {
        def predefined = predefined.find { it.isSame(id, method, params) }
        ResponseJson json = new ResponseJson<Object, Integer>(id: id)
        if (predefined != null) {
            json.result = predefined.result
        } else {
            json.error = new RpcResponseError(-32601, "Method ${method} with ${params} is not mocked")
        }
        return Mono.just(objectMapper.writeValueAsBytes(json))
    }

    class PredefinedResponse {
        String method
        List params
        Object result

        boolean isSame(int id, String method, List<?> params) {
            if (method != this.method) {
                return false
            }
            if (this.params == null) {
                return true
            }
            return this.params == params
        }
    }
}