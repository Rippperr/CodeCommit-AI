package com.codecommitai.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubContentResponse(

        String name,

        String path,

        String sha,

        String type,

        Long size,

        @JsonProperty("download_url")
        String downloadUrl,

        @JsonProperty("html_url")
        String htmlUrl
) {
}