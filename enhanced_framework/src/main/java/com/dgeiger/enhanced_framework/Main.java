package com.dgeiger.enhanced_framework;

import com.dgeiger.enhanced_framework.apps.App;
import com.dgeiger.enhanced_framework.filtering.FilterApp;
import com.dgeiger.enhanced_framework.filtering.FilterLayer;
import com.dgeiger.enhanced_framework.proxy.ProxyNetworkSettings;
import com.dgeiger.enhanced_framework.proxy.RoutingProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static void buildProxyChain() {
        boolean saveTimestamps = FrameworkProperties.getBoolean("saveTimestamps");
        List<App> apps = buildAppList();
        int upstreamPort = FrameworkProperties.getInteger("upstreamPort");
        int downstreamPort = FrameworkProperties.getInteger("downstreamPort");
        String upstreamIp = FrameworkProperties.getString("upstreamIp");
        boolean useTls = FrameworkProperties.getBoolean("useTls");

        Collections.reverse(apps);
        int currentDownStreamPort = 9001;
        int lastDownstreamPort = upstreamPort;
        for(int i = 0; i < apps.size(); i++){
            if(i == apps.size() - 1){
                currentDownStreamPort = downstreamPort;
            }

            ProxyNetworkSettings proxyNetworkSettings = new ProxyNetworkSettings()
                    .setDownstreamPort(currentDownStreamPort)
                    .setUpstreamPort(lastDownstreamPort)
                    .setUseSsl(useTls);

            RoutingProxy.SaveTimestampsSetting saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.NONE;

            if(i == 0){
                proxyNetworkSettings.setUpstreamIp(upstreamIp);
                if(saveTimestamps) saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.UPSTREAM;
            }
            if(i == apps.size() - 1){
                if(i == 0 && saveTimestamps) saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.ALL;
                else if(saveTimestamps) saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.DOWNSTREAM;
            }

            new RoutingProxy(proxyNetworkSettings,
                    Collections.singletonList(apps.get(i)), saveTimestampsSetting).listen();

            lastDownstreamPort = currentDownStreamPort;
            currentDownStreamPort++;
            while(currentDownStreamPort == downstreamPort || currentDownStreamPort == upstreamPort){
                currentDownStreamPort++;
            }
        }
    }

    private static void buildServiceChain() {
        boolean saveTimestamps = FrameworkProperties.getBoolean("saveTimestamps");
        ProxyNetworkSettings proxyNetworkSettings = new ProxyNetworkSettings()
                .setDownstreamPort(FrameworkProperties.getInteger("downstreamPort"))
                .setUpstreamPort(FrameworkProperties.getInteger("upstreamPort"))
                .setUpstreamIp(FrameworkProperties.getString("upstreamIp"))
                .setUseSsl(FrameworkProperties.getBoolean("useTls"));

        RoutingProxy.SaveTimestampsSetting saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.NONE;
        if(saveTimestamps) saveTimestampsSetting = RoutingProxy.SaveTimestampsSetting.ALL;

        new RoutingProxy(proxyNetworkSettings, buildAppList(), saveTimestampsSetting).listen();
    }

    private static ArrayList<App> buildAppList(){
        boolean matchAgainstAtLeastOneFilter = FrameworkProperties.getBoolean("matchAgainstAtLeastOneFilter");
        boolean useCache = FrameworkProperties.getBoolean("useCache");
        ArrayList<App> apps = new ArrayList<>();

        String appParameter = FrameworkProperties.getString("appIntegerParameter");

        for(String gcmiAppClassName : FrameworkProperties.getStringArray("gcmiApps")){
            try {
                Class gcmiAppClass = Class.forName(gcmiAppClassName);
                App gcmiApp;
                if(appParameter == null || appParameter.isEmpty()){
                    gcmiApp = (App) Class.forName(gcmiAppClassName).getDeclaredConstructor().newInstance();
                }else{
                    Class[] cArg = new Class[1];
                    cArg[0] = Integer.class;
                    gcmiApp = (App) Class.forName(gcmiAppClassName).getDeclaredConstructor(cArg)
                            .newInstance(Integer.parseInt(appParameter));
                }

                if(FilterApp.class.isAssignableFrom(gcmiAppClass)){
                    apps.add(new FilterLayer((FilterApp) gcmiApp, matchAgainstAtLeastOneFilter, useCache));
                }else{
                    apps.add(gcmiApp);
                }
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        log.debug("Starting " + apps.size() + " GCMI Apps");

        return apps;
    }

    public static void main(String[] args) {
        String configFilePath = "src/main/resources/config.properties";
        if(args.length > 0) configFilePath = args[0];

        try {
            FrameworkProperties.getInstance().load(configFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(FrameworkProperties.getBoolean("multipleProxies")){
            log.info("Starting enhanced framework with multiple proxies, streaming to "
                    + FrameworkProperties.getString("upstreamIp"));
            buildProxyChain();
        }else{
            log.info("Starting enhanced framework with one proxy, streaming to "
                    + FrameworkProperties.getString("upstreamIp"));
            buildServiceChain();
        }

    }

}
