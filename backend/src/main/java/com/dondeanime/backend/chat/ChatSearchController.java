package com.dondeanime.backend.chat;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatSearchController {

    private final ChatSearchService chatSearchService;

    public ChatSearchController(ChatSearchService chatSearchService) {
        this.chatSearchService = chatSearchService;
    }

    @PostMapping("/api/chat/search")
    public ChatSearchResponse search(@RequestBody ChatSearchRequest request) {
        return chatSearchService.search(request);
    }
}
