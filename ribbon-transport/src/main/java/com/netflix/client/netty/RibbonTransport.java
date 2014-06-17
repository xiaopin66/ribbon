package com.netflix.client.netty;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.client.DefaultLoadBalancerRetryHandler;
import com.netflix.client.RetryHandler;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.netty.http.NettyHttpClient;
import com.netflix.client.netty.http.NettyHttpLoadBalancerErrorHandler;
import com.netflix.client.netty.http.SSEClient;
import com.netflix.client.netty.tcp.LoadBalancingTcpClient;
import com.netflix.client.netty.udp.LoadBalancingUdpClient;
import com.netflix.config.DynamicIntProperty;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.utils.ScheduledThreadPoolExectuorWithDynamicSize;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.client.RxClient;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

public final class RibbonTransport {
    
    protected static final PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<ByteBuf>> DEFAULT_SSE_PIPELINE_CONFIGURATOR  = 
            PipelineConfigurators.sseClientConfigurator();

    protected static final PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<ByteBuf>> DEFAULT_HTTP_PIPELINE_CONFIGURATOR = 
            PipelineConfigurators.httpClientConfigurator();
    
    private static final DynamicIntProperty POOL_CLEANER_CORE_SIZE = new DynamicIntProperty("rxNetty.poolCleaner.coreSize", 2);

    private static final ScheduledExecutorService poolCleanerScheduler;

    static {
        ThreadFactory factory = (new ThreadFactoryBuilder()).setDaemon(true)
                .setNameFormat("RxClient_Connection_Pool_Clean_Up")
                .build();
        poolCleanerScheduler = new ScheduledThreadPoolExectuorWithDynamicSize(POOL_CLEANER_CORE_SIZE, factory);
    }

    private RibbonTransport() {
    }
 
    private static RetryHandler getDefaultHttpRetryHandlerWithConfig(IClientConfig config) {
        return new NettyHttpLoadBalancerErrorHandler(config);
    }
    
    private static RetryHandler getDefaultRetryHandlerWithConfig(IClientConfig config) {
        return new DefaultLoadBalancerRetryHandler(config);
    }
    
    public static RxClient<ByteBuf, ByteBuf> newTcpClient(ILoadBalancer loadBalancer, IClientConfig config) {
        return new LoadBalancingTcpClient<ByteBuf, ByteBuf>(loadBalancer, config, getDefaultRetryHandlerWithConfig(config), null, poolCleanerScheduler);
    }
    
    public static <I, O> RxClient<I, O> newTcpClient(ILoadBalancer loadBalancer, PipelineConfigurator<O, I> pipelineConfigurator, 
            IClientConfig config, RetryHandler retryHandler) {
        return new LoadBalancingTcpClient<I, O>(loadBalancer, config, retryHandler, pipelineConfigurator, poolCleanerScheduler);
    }
    
    public static <I, O> RxClient<I, O> newTcpClient(PipelineConfigurator<O, I> pipelineConfigurator, 
            IClientConfig config) {
        return new LoadBalancingTcpClient<I, O>(config, getDefaultRetryHandlerWithConfig(config), pipelineConfigurator, poolCleanerScheduler);    
    }

    public static RxClient<ByteBuf, ByteBuf> newTcpClient(IClientConfig config) {
        return new LoadBalancingTcpClient<ByteBuf, ByteBuf>(config, getDefaultRetryHandlerWithConfig(config), null, poolCleanerScheduler);    
    }
 
    public static RxClient<DatagramPacket, DatagramPacket> newUdpClient(ILoadBalancer loadBalancer, IClientConfig config) {
        return new LoadBalancingUdpClient<DatagramPacket, DatagramPacket>(loadBalancer, config, getDefaultRetryHandlerWithConfig(config), null);
    }
 
    public static RxClient<DatagramPacket, DatagramPacket> newUdpClient(IClientConfig config) {
        return new LoadBalancingUdpClient<DatagramPacket, DatagramPacket>(config, getDefaultRetryHandlerWithConfig(config), null);
    }
    
    public static <I, O> RxClient<I, O> newUdpClient(ILoadBalancer loadBalancer, PipelineConfigurator<O, I> pipelineConfigurator, 
            IClientConfig config, RetryHandler retryHandler) {
        return new LoadBalancingUdpClient<I, O>(loadBalancer, config, retryHandler, pipelineConfigurator);
    }
    
    public static <I, O> RxClient<I, O> newUdpClient(PipelineConfigurator<O, I> pipelineConfigurator, IClientConfig config) {
        return new LoadBalancingUdpClient<I, O>(config, getDefaultRetryHandlerWithConfig(config), pipelineConfigurator);
    }

    public static HttpClient<ByteBuf, ByteBuf> newHttpClient() {
        IClientConfig config = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
        return newHttpClient(config);
    }
     
    public static HttpClient<ByteBuf, ByteBuf> newHttpClient(ILoadBalancer loadBalancer, IClientConfig config) {
        return new NettyHttpClient<ByteBuf, ByteBuf>(loadBalancer, config, getDefaultHttpRetryHandlerWithConfig(config), DEFAULT_HTTP_PIPELINE_CONFIGURATOR, poolCleanerScheduler);
    }
    
    public static HttpClient<ByteBuf, ByteBuf> newHttpClient(ILoadBalancer loadBalancer, IClientConfig config, RetryHandler retryHandler) {
        return new NettyHttpClient<ByteBuf, ByteBuf>(loadBalancer, config, retryHandler, DEFAULT_HTTP_PIPELINE_CONFIGURATOR, poolCleanerScheduler);
    }
 
    public static HttpClient<ByteBuf, ByteBuf> newHttpClient(IClientConfig config) {
        return new NettyHttpClient<ByteBuf, ByteBuf>(config, getDefaultHttpRetryHandlerWithConfig(config), DEFAULT_HTTP_PIPELINE_CONFIGURATOR, poolCleanerScheduler);
    }
    
    public static <I, O> HttpClient<I, O> newHttpClient(PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator, 
            ILoadBalancer loadBalancer, IClientConfig config) {
        return new NettyHttpClient<I, O>(loadBalancer, config, getDefaultHttpRetryHandlerWithConfig(config), pipelineConfigurator, poolCleanerScheduler);
    }
    
    public static <I, O> HttpClient<I, O> newHttpClient(PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator, 
            IClientConfig config) {
        return new NettyHttpClient<I, O>(config, getDefaultHttpRetryHandlerWithConfig(config), pipelineConfigurator, poolCleanerScheduler);
    }
    
    public static <I, O> HttpClient<I, O> newHttpClient(PipelineConfigurator<HttpClientResponse<O>, HttpClientRequest<I>> pipelineConfigurator, 
            IClientConfig config, RetryHandler retryHandler) {
        return new NettyHttpClient<I, O>(config, retryHandler, pipelineConfigurator, poolCleanerScheduler);
    }
    
    public static HttpClient<ByteBuf, ServerSentEvent> newSSEClient(ILoadBalancer loadBalancer, IClientConfig config) {
        return new SSEClient<ByteBuf>(loadBalancer, config, getDefaultHttpRetryHandlerWithConfig(config), DEFAULT_SSE_PIPELINE_CONFIGURATOR);
    }
 
    public static HttpClient<ByteBuf, ServerSentEvent> newSSEClient(IClientConfig config) {
        return new SSEClient<ByteBuf>(config, getDefaultHttpRetryHandlerWithConfig(config), DEFAULT_SSE_PIPELINE_CONFIGURATOR);
    }
    
    public static <I> HttpClient<I, ServerSentEvent> newSSEClient(PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>> pipelineConfigurator, 
            ILoadBalancer loadBalancer, IClientConfig config) {
        return new SSEClient<I>(loadBalancer, config, getDefaultHttpRetryHandlerWithConfig(config), pipelineConfigurator);
    }
    
    public static <I> HttpClient<I, ServerSentEvent> newSSEClient(PipelineConfigurator<HttpClientResponse<ServerSentEvent>, HttpClientRequest<I>> pipelineConfigurator, 
            IClientConfig config) {
        return new SSEClient<I>(config, getDefaultHttpRetryHandlerWithConfig(config), pipelineConfigurator);
    }

    public static HttpClient<ByteBuf, ServerSentEvent> newSSEClient() {
        IClientConfig config = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
        return newSSEClient(config);
    }
}

