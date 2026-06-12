package com.dondeanime.backend.llm;

class DisabledLlmClient implements LlmClient {

    private final String model;

    DisabledLlmClient(String model) {
        this.model = model == null || model.isBlank() ? "disabled" : model.trim();
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public LlmCompletion complete(LlmRequest request) {
        throw new LlmClientDisabledException();
    }
}
