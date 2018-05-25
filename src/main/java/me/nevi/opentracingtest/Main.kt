package me.nevi.opentracingtest

import io.grpc.examples.routeguide.Point
import io.jaegertracing.Configuration
import io.opentracing.Tracer
import io.opentracing.util.GlobalTracer
import me.nevi.opentracingtest.rpc.RouteGuideClient
import me.nevi.opentracingtest.rpc.RouteGuideServer

fun main(args: Array<String>) {

    val tracingConfig = Configuration("opentracing-test")
    val tracer: Tracer = tracingConfig.tracer

    GlobalTracer.register(tracer)

    val server = RouteGuideServer("0.0.0.0", 8081)

    server.start()

    val client = RouteGuideClient("localhost", 8081, true)

    val feature = client.getBlockingStub().getFeature(Point.getDefaultInstance())

    println(feature)

    // run a few times
    client.getBlockingStub().apply {
        (0..100).forEach {
            getFeature(Point.getDefaultInstance())
        }
    }

    Thread.sleep(15_000)

    server.stop()

}