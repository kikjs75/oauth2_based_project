package com.portfolio.fcm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FcmConfigTest {

    @Test
    @DisplayName("sendEndpoint 미설정 → projectId 기반 기본 URL 반환")
    void resolvedSendEndpoint_usesDefaultUrlWhenNotSet() {
        FcmConfig config = new FcmConfig("test-project", null);
        assertThat(config.resolvedSendEndpoint())
                .isEqualTo("https://fcm.googleapis.com/v1/projects/test-project/messages:send");
    }

    @Test
    @DisplayName("sendEndpoint 설정 시 해당 URL 반환")
    void resolvedSendEndpoint_usesCustomUrlWhenSet() {
        FcmConfig config = new FcmConfig("test-project", "http://custom-endpoint/send");
        assertThat(config.resolvedSendEndpoint()).isEqualTo("http://custom-endpoint/send");
    }

    @Test
    @DisplayName("projectId와 sendEndpoint 모두 null → IllegalArgumentException")
    void constructor_throwsWhenBothProjectIdAndSendEndpointAreNull() {
        assertThatThrownBy(() -> new FcmConfig(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");
    }
}
