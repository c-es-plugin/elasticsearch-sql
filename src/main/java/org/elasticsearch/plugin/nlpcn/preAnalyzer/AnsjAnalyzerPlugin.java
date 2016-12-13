package org.elasticsearch.plugin.nlpcn.preAnalyzer;

import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;

/**
 * Created by fangbb on 2016-12-6.
 */
public class AnsjAnalyzerPlugin extends AbstractComponent {
    @Inject
    public AnsjAnalyzerPlugin(final Settings settings){
        super(settings);
        AnsjElasticConfigurator.init(settings);
    }
}
