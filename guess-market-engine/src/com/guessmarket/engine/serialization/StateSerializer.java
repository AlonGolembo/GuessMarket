package com.guessmarket.engine.serialization;

import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.Event;

import java.io.*;
import java.util.List;

public class StateSerializer {

    public static void saveEngineState(List<Event> events, String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Save file path cannot be empty.");
        }

        File file = new File(filePath.trim());
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(events);
        } catch (IOException e) {
            throw new MarketException("Failed to save state to file [" + filePath + "]: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Event> loadEngineState(String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Load file path cannot be empty.");
        }

        File file = new File(filePath.trim());
        if (!file.exists()) {
            throw new MarketException("State file does not exist at path: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Event>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new MarketException("Failed to load saved state from file [" + filePath + "]: " + e.getMessage(), e);
        }
    }
}