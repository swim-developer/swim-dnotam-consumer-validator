package com.github.swim_developer.validator.dnotam.consumer.domain.port.in;

import com.github.swim_developer.validator.core.domain.model.TopicDetails;
import com.github.swim_developer.validator.core.domain.model.TopicSummary;

import java.util.List;
import java.util.Optional;

public interface DnotamTopicPort {
    List<TopicSummary> getAllTopics();
    Optional<TopicDetails> getTopicDetails(String topicId);
}
