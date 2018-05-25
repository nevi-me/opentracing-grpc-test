package me.nevi.opentracingtest.rpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.examples.routeguide.RouteGuideGrpc
import io.grpc.stub.MetadataUtils
import io.opentracing.contrib.grpc.ClientTracingInterceptor
import io.opentracing.util.GlobalTracer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class RouteGuideClient(channelBuilder: ManagedChannelBuilder<*>) {
    private val channel: ManagedChannel
    private var databaseBlockingStub: RouteGuideGrpc.RouteGuideBlockingStub
    private var databaseStub: RouteGuideGrpc.RouteGuideStub

    constructor(host: String, port: Int, plainText: Boolean = true) : this(
            when(plainText) {
                true -> ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext(true)
                        .executor(Executors.newFixedThreadPool(8))
                false -> ManagedChannelBuilder.forAddress(host, port)
                        .useTransportSecurity()
                        .executor(Executors.newFixedThreadPool(8))
            }
    )

    init {
        val metadata: io.grpc.Metadata = io.grpc.Metadata()
        channel = channelBuilder.build()

        val tracingInterceptor = ClientTracingInterceptor(GlobalTracer.get())

        databaseBlockingStub = MetadataUtils.attachHeaders(RouteGuideGrpc.newBlockingStub(tracingInterceptor.intercept(channel)), metadata)
        databaseStub = MetadataUtils.attachHeaders(RouteGuideGrpc.newStub(tracingInterceptor.intercept(channel)), metadata)
    }

    @Throws(InterruptedException::class)
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun isShutdown() : Boolean {
        return channel.isShutdown
    }

    fun getBlockingStub() : RouteGuideGrpc.RouteGuideBlockingStub {
        if (channel.isShutdown) {
            val metadata: io.grpc.Metadata = io.grpc.Metadata()
            metadata.put(Metadata.Key.of("My-Header", Metadata.ASCII_STRING_MARSHALLER), "my-header-value")
            databaseBlockingStub = MetadataUtils.attachHeaders(RouteGuideGrpc.newBlockingStub(channel), metadata)
            return databaseBlockingStub
        }
        return databaseBlockingStub
    }

    fun getStub() : RouteGuideGrpc.RouteGuideStub {
        if (channel.isShutdown) {
            val metadata: Metadata = Metadata()
            metadata.put(Metadata.Key.of("My-Header", Metadata.ASCII_STRING_MARSHALLER), "my-header-value")
            databaseStub = MetadataUtils.attachHeaders(RouteGuideGrpc.newStub(channel), metadata)
            return databaseStub
        }
        return databaseStub
    }
}