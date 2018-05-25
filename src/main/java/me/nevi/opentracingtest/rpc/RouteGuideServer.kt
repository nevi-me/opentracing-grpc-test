package me.nevi.opentracingtest.rpc

import io.grpc.*
import io.grpc.examples.routeguide.Feature
import io.grpc.examples.routeguide.Point
import io.grpc.examples.routeguide.RouteGuideGrpc
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.opentracing.contrib.grpc.ServerTracingInterceptor
import io.opentracing.util.GlobalTracer
import java.io.IOException
import java.net.InetSocketAddress
import java.util.logging.Logger

class RouteGuideServer(serverBuilder: ManagedChannel, host: String, port: Int) {
    private val server: Server

    /**
     * create server to listen on {@code host} {@code port}.
     */
    @Throws(IOException::class)
    constructor(host: String, port: Int) : this(NettyChannelBuilder.forAddress(host, port).usePlaintext().build(), host, port)

    init {
        val tracingInterceptor = ServerTracingInterceptor(GlobalTracer.get())

        // I can't find a way of specifying this when creating a tracer
        val tracedAttributes = setOf(
                ServerTracingInterceptor.ServerRequestAttribute.CALL_ATTRIBUTES,
                ServerTracingInterceptor.ServerRequestAttribute.HEADERS,
                ServerTracingInterceptor.ServerRequestAttribute.METHOD_NAME,
                ServerTracingInterceptor.ServerRequestAttribute.METHOD_TYPE
        )

        server = NettyServerBuilder.forAddress(InetSocketAddress(host, port)).apply {
            addService(tracingInterceptor.intercept(RouteGuideService()))
        }.build()
    }

    /**
     * Start serving requests.
     */
    @Throws(IOException::class)
    fun start() {
        server.start()
        logger.info("Server started, listening on ${server.port}")
        Runtime.getRuntime().addShutdownHook(object:Thread() {
            override fun run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down")
                this@RouteGuideServer.stop()
                System.err.println("*** server shut down")
            }
        })
    }

    /**
     * Stop serving requests and shutdown resources.
     */
    fun stop() {
        server.shutdown()
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    @Throws(InterruptedException::class)
    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    private class RouteGuideService: RouteGuideGrpc.RouteGuideImplBase() {

        override fun getFeature(request: Point, responseObserver: StreamObserver<Feature>) {

            val feature = Feature.newBuilder().setName("Test Feature")
            val tracer = GlobalTracer.get()

            println("Expecting an active span, but found ${tracer.activeSpan()}")
            responseObserver.onNext(feature.build())
            responseObserver.onCompleted()
        }
    }

    companion object {
        private val logger = Logger.getLogger(RouteGuideServer::class.java.name)
    }

}