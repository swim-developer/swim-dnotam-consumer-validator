package com.github.swim_developer.validator.dnotam.consumer.infrastructure.config;

import com.github.swim_developer.validator.consumer.domain.model.SubscriptionEntity;
import com.github.swim_developer.validator.core.domain.model.QualityOfService;
import com.github.swim_developer.validator.core.domain.model.SubscriptionStatus;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.FilterOptions;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = {
        FilterOptions.class,
        EventFileMetadata.class,
        SubscriptionEntity.class,
        SubscriptionStatus.class,
        QualityOfService.class
})
@TemplateData(target = FilterOptions.class)
@TemplateData(target = EventFileMetadata.class)
@TemplateData(target = SubscriptionEntity.class)
@TemplateData(target = SubscriptionStatus.class)
@TemplateData(target = QualityOfService.class)
public class DnotamReflectionConfiguration {
}
