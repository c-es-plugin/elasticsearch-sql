package org.elasticsearch.plugin.nlpcn;


import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugin.nlpcn.preAnalyzer.AnsjAnalyzerPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;
import java.util.Collections;

public class SqlPlug extends Plugin {

	public SqlPlug() {
	}

	@Override
	public String name() {
		return "sql";
	}

	@Override
	public String description() {
		return "Use sql to query elasticsearch.";
	}
	@Override
	public Collection<Module> nodeModules() {
		return Collections.<Module> singletonList(new AnsjModule());
	}
	public static class AnsjModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(AnsjAnalyzerPlugin.class).asEagerSingleton();
		}
	}

	public void onModule(RestModule module)
	{
		module.addRestAction(RestSqlAction.class);
	}
}
