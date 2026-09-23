package com.guessmarket.server;

import com.guessmarket.dto.EventDTO;
import com.guessmarket.dto.EventDetailsDTO;
import com.guessmarket.dto.NewEventDTO;
import com.guessmarket.engine.api.MarketEngine;
import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.server.dto.ActivateEventRequest;
import com.guessmarket.server.dto.CloseEventRequest;
import com.guessmarket.server.dto.UploadResultResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Event reads and commands: list all / active only / one event's details,
 * create, upload (multipart, held in memory only - never written to disk),
 * activate, close. One servlet dispatching on {@code pathInfo} rather than a
 * class per action.
 */
@WebServlet(urlPatterns = "/api/events/*")
@MultipartConfig
public class EventsServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pathInfo = request.getPathInfo();
        handle(request, response, () -> {
            MarketEngine engine = engine(request);
            if (pathInfo == null || pathInfo.equals("/")) {
                List<EventDTO> events = engine.getAllEvents();
                JsonUtil.writeJson(response, events);
            } else if (pathInfo.equals("/active")) {
                List<EventDTO> events = engine.getActiveEvents();
                JsonUtil.writeJson(response, events);
            } else if (pathInfo.equals("/details")) {
                String name = require(request.getParameter("name"), "name");
                EventDetailsDTO details = engine.getEventDetails(name);
                JsonUtil.writeJson(response, details);
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
                NewEventDTO spec = JsonUtil.readBody(request, NewEventDTO.class);
                EventDTO created = engine(request).createEvent(spec);
                JsonUtil.writeJson(response, created);
            });
        } else if (pathInfo.equals("/upload")) {
            handle(request, response, () -> uploadEvents(request, response));
        } else if (pathInfo.equals("/activate")) {
            handle(request, response, () -> {
                ActivateEventRequest body = JsonUtil.readBody(request, ActivateEventRequest.class);
                engine(request).activateEvent(body.eventName(), body.userName());
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
        } else if (pathInfo.equals("/close")) {
            handle(request, response, () -> {
                CloseEventRequest body = JsonUtil.readBody(request, CloseEventRequest.class);
                engine(request).closeEvent(body.eventName(), body.winningOptionIndex1Based());
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            });
        } else {
            JsonUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, "Unknown path: " + pathInfo);
        }
    }

    /**
     * Reads the uploaded file's bytes straight into memory - {@code Part} is
     * backed by a request-scoped buffer/temp area managed by the container,
     * never written anywhere by this code - and hands the content straight to
     * the engine as a string. Nothing is ever saved to disk.
     */
    private void uploadEvents(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String uploaderName = require(request.getParameter("uploaderName"), "uploaderName");
        Part filePart;
        try {
            filePart = request.getPart("file");
        } catch (ServletException e) {
            throw new MarketException("Could not read the uploaded file: " + e.getMessage());
        }
        if (filePart == null) {
            throw new MarketException("Missing 'file' part.");
        }
        String xmlContent;
        try (InputStream in = filePart.getInputStream()) {
            xmlContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> addedEventNames = engine(request).uploadEventsXml(uploaderName, xmlContent);
        JsonUtil.writeJson(response, new UploadResultResponse(addedEventNames));
    }
}
