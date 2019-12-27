package com.dataexpo.dataexpocamera.netty;

import java.nio.ByteOrder;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.IdleStateHandler;

public class UdpChannelInitializer extends ChannelInitializer<DatagramChannel> {
    private UdpChannelInboundHandler inboundHandler;
    public UdpChannelInitializer(UdpChannelInboundHandler handler){
        inboundHandler = handler;
    }

    @Override
    protected void initChannel(DatagramChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new IdleStateHandler(12,15,0));
        pipeline.addLast(inboundHandler);
        //pipeline.addLast(new LengthFieldPrepender(4));
//        pipeline.addLast(new LengthFieldBasedFrameDecoder(ByteOrder.LITTLE_ENDIAN, Integer.MAX_VALUE,
//                0, 4, 0, 4, true));
//        pipeline.addLast(new LengthFieldPrepender(ByteOrder.LITTLE_ENDIAN, 4, 0, false));
    }
}
