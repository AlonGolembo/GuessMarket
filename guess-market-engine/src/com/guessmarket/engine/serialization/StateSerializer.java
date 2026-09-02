package com.guessmarket.engine.serialization;

import com.guessmarket.engine.exception.MarketException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/** Binary save/load of the whole {@link MarketSnapshot}. */
public final class StateSerializer {

    private StateSerializer() {}

    public static void save(MarketSnapshot snapshot, String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Save file path cannot be empty.");
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(filePath.trim())))) {
            out.writeObject(snapshot);
        } catch (IOException e) {
            throw new MarketException("Failed to save state to [" + filePath + "]: " + e.getMessage(), e);
        }
    }

    public static MarketSnapshot load(String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Load file path cannot be empty.");
        }
        File file = new File(filePath.trim());
        if (!file.exists()) {
            throw new MarketException("State file does not exist at path: " + filePath);
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return (MarketSnapshot) in.readObject();
        } catch (StreamCorruptedException | ClassCastException e) {
            throw new MarketException(
                    "That file is not a valid Guess Market saved state (did you pick an XML file by mistake?).");
        } catch (ClassNotFoundException | InvalidClassException e) {
            throw new MarketException("The state file was written by an incompatible version of the system.");
        } catch (IOException e) {
            throw new MarketException("Failed to load saved state from [" + filePath + "]: " + e.getMessage(), e);
        }
    }
}
