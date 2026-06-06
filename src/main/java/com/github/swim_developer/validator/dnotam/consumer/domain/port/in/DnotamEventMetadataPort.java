package com.github.swim_developer.validator.dnotam.consumer.domain.port.in;

import com.github.swim_developer.validator.dnotam.consumer.domain.model.EventFileMetadata;
import com.github.swim_developer.validator.dnotam.consumer.domain.model.FilterOptions;

import java.util.List;

public interface DnotamEventMetadataPort {
    List<EventFileMetadata> getAllEventMetadata();
    FilterOptions getFilterOptions();
}
