package com.guessmarket.server;

import com.guessmarket.server.dto.PostChatMessageRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Global chat shared by every connected client: GET returns the full
 * history (oldest first), POST appends a message. The poster's name must
 * already be a logged-in user (checked via the shared {@code MarketEngine},
 * same identity model every other write endpoint relies on).
 */
@WebServlet(urlPatterns = "/api/chat/*")
public class ChatServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        handle(request, response, () -> {
            if (pathInfo == null || pathInfo.equals("/")) {
                JsonUtil.writeJson(response, chatStore(request).all());
            } else {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            handle(request, response, () -> {
                PostChatMessageRequest body = JsonUtil.readBody(request, PostChatMessageRequest.class);
                String userName = require(body.userName(), "userName");
                String text = require(body.text(), "text");
                engine(request).getUserDetails(userName); // validates the name is a logged-in user
                ChatMessage posted = chatStore(request).post(userName, text);
                JsonUtil.writeJson(response, posted);
            });
        } else {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
        }
    }
}
