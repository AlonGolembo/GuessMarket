package com.guessmarket.server;

import com.guessmarket.dto.UserDTO;
import com.guessmarket.dto.UserDetailsDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.server.dto.DepositRequest;
import com.guessmarket.server.dto.LoginRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * User reads and account commands: list all, one user's details, login, deposit.
 */
@WebServlet(urlPatterns = "/api/users/*")
public class UsersServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        handle(request, response, () -> {
            MarketEngine engine = engine(request);
            if (pathInfo == null || pathInfo.equals("/")) {
                Map<String, UserDTO> users = engine.getAllUsers();
                JsonUtil.writeJson(response, users);
            } else if (pathInfo.equals("/details")) {
                String name = require(request.getParameter("name"), "name");
                UserDetailsDTO details = engine.getUserDetails(name);
                JsonUtil.writeJson(response, details);
            } else {
                JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.equals("/login")) {
            handle(request, response, () -> {
                LoginRequest body = JsonUtil.readBody(request, LoginRequest.class);
                UserDTO user = engine(request).login(body.name());
                JsonUtil.writeJson(response, user);
            });
        } else if (pathInfo != null && pathInfo.equals("/deposit")) {
            handle(request, response, () -> {
                DepositRequest body = JsonUtil.readBody(request, DepositRequest.class);
                engine(request).deposit(body.userName(), body.amount());
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
        } else {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
        }
    }
}
