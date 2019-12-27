package com.dataexpo.dataexpocamera.netty;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;

import com.dataexpo.dataexpocamera.activity.Frame;
import com.dataexpo.dataexpocamera.comm.FileUtils;
import com.dataexpo.dataexpocamera.comm.JsonUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@ChannelHandler.Sharable
public class UDPClient extends UdpChannelInboundHandler implements Runnable {
    private final static String TAG = UDPClient.class.getSimpleName();
    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private UdpChannelInitializer udpChannelInitializer;
    private ExecutorService executorService;
    Channel channel = null;

    public UDPClient() {
        init();
    }

    private void init() {
        bootstrap = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioDatagramChannel.class);
                //.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(Integer.MAX_VALUE));
//                .option(ChannelOption.SO_RCVBUF,1024)
                //.option(ChannelOption.SO_SNDBUF,Integer.MAX_VALUE);

        udpChannelInitializer = new UdpChannelInitializer(this);
        bootstrap.handler(udpChannelInitializer);

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(this);
    }

    @Override
    public void receive(String data) {
        Log.d(TAG, "client rcv : " + data);
    }

    @Override
    public void run() {
        try {
            ChannelFuture channelFuture = bootstrap.bind(19123).sync();
            channel = channelFuture.channel();
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void sendTo(byte[] data, int width, int height) {
        Frame frame = new Frame();
        frame.width = width;
        frame.height = height;
        frame.time = new Date().getTime();
        String write = "";

        //将yuv转化为jpeg

        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21,640, 480, null);
        yuvImage.compressToJpeg(new Rect(0, 0, 640, 480), 60, baos);
//
//        String str = "";
//        for (int i = 0; i < 100; i++) {
//            str += baos.toByteArray()[i] + " ";
//        }
//        Log.d(TAG, "str is " + str);
//
//        Log.d(TAG, baos.toString());
        //baos.toByteArray();

        //frame.data = new byte[3];
        frame.data = FileUtils.toBase64(baos.toByteArray());

        Log.i(TAG, "data.length " + baos.toByteArray().length);
        try {
            baos.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            write = JsonUtil.getInstance().obj2json(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "channel: " + channel);
        if (channel != null) {
            channel.writeAndFlush(new DatagramPacket(Unpooled.copiedBuffer(write, CharsetUtil.UTF_8),
                    new InetSocketAddress("192.168.1.22", 19123)));
//                    .
//                    addListener(new GenericFutureListener<Future<? super Void>>() {
//                        @Override
//                        public void operationComplete(Future<? super Void> future) throws Exception {
//                            Log.d(TAG, "send end! ");
//                        }
//                    });
        }
//
//        send(new DatagramPacket(Unpooled.copiedBuffer(write, CharsetUtil.UTF_8),
//                new InetSocketAddress("192.168.1.22",19123)));
    }
}
