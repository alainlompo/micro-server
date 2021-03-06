package com.aol.micro.server.servers;

import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import com.aol.cyclops.control.ReactiveSeq;
import com.aol.cyclops.util.stream.StreamUtils;
import com.aol.micro.server.Plugin;
import com.aol.micro.server.PluginLoader;
import com.aol.micro.server.module.IncorrectJaxRsPluginsException;
import com.aol.micro.server.rest.RestConfiguration;
import com.aol.micro.server.servers.model.ServerData;

public class JaxRsServletConfigurer {
	public  void addServlet(ServerData serverData, ServletContext webappContext) {
		
		List<RestConfiguration> restConfigList = ReactiveSeq.fromStream(PluginLoader.INSTANCE.plugins.get().stream())
				.filter(module -> module.restServletConfiguration()!=null)
				.map(Plugin::restServletConfiguration)
				.flatMap(StreamUtils::optionalToStream)
				.toList();
		if(restConfigList.size()>1) {
			throw new IncorrectJaxRsPluginsException("ERROR!  Multiple jax-rs application plugins found " + restConfigList);
		}else if(restConfigList.size()==0){
			throw new IncorrectJaxRsPluginsException("ERROR!  No jax-rs application plugins found ");
		}
		
		RestConfiguration config = restConfigList.get(0);
		javax.servlet.ServletRegistration.Dynamic servletRegistration = webappContext.addServlet(config.getName()
					+"-"+serverData.getModule().getContext(),config.getServlet());
		Map<String,String> initParams = config.getInitParams();
		for(String key : initParams.keySet()){
			servletRegistration.setInitParameter(key,initParams.get(key));
		}
		servletRegistration.setAsyncSupported(true);
		servletRegistration.setInitParameter("javax.ws.rs.Application", serverData.getModule().getJaxWsRsApplication());
		servletRegistration.setInitParameter(config.getProvidersName(), serverData.getModule().getProviders());
		servletRegistration.setInitParameter("context", serverData.getModule().getContext());
		servletRegistration.setLoadOnStartup(1);
		servletRegistration.addMapping(serverData.getBaseUrlPattern());
	}
	
}
