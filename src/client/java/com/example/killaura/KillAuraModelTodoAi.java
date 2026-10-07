/* LiquidBounce combat models and inference architecture, copyright CCBlueX 2015-2026.
 * Adapted under GPL-3.0-or-later. See KillAuraLicenseTodoAi.txt. */
package com.example.killaura;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/** CPU inference for LiquidBounce's fixed 6 -> 128 -> 64 -> 32 -> 2 MLP.
 * Reads FLOAT32 DJL parameter tensors; requires no engine, downloads, or native libraries.
 * This intentionally supports inference only, not the upstream model-training framework. */
public final class KillAuraModelTodoAi {
    private record Tensor(String name, long[] shape, float[] values) {}
    private final List<Tensor> parameters;
    private static final int MAX_BYTES = 1_048_576;
    private static final int[] WIDTHS = {6, 128, 64, 32, 2};

    private KillAuraModelTodoAi(List<Tensor> parameters) { this.parameters = List.copyOf(parameters); }

    public static KillAuraModelTodoAi load(InputStream source) throws IOException {
        if (source == null) throw new IOException("Model not found");
        byte[] bytes = source.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES || bytes.length < 8 || bytes[0] != 'D' || bytes[1] != 'J' || bytes[2] != 'L' || bytes[3] != '@')
            throw new IOException("Expected a DJL combat parameter file, up to 1 MiB");
        List<Tensor> tensors = new ArrayList<>();
        // The block metadata is engine-specific. Each tensor has a self-contained NDAR record;
        // skip its entire payload so float data can never be mistaken for another record.
        for (int offset = 8; offset + 6 < bytes.length; offset++) {
            if (bytes[offset] != 0 || bytes[offset + 1] != 4 || bytes[offset + 2] != 'N' || bytes[offset + 3] != 'D'
                    || bytes[offset + 4] != 'A' || bytes[offset + 5] != 'R') continue;
            ByteArrayInputStream remaining = new ByteArrayInputStream(bytes, offset, bytes.length - offset);
            DataInputStream input = new DataInputStream(remaining);
            input.readUTF();
            int version = input.readInt();
            if (version != 3) throw new IOException("Unsupported tensor version: " + version);
            int named = input.readUnsignedByte();
            if (named != 0 && named != 1) throw new IOException("Invalid tensor name flag");
            String name = named == 1 ? input.readUTF() : "";
            if (!input.readUTF().equals("DENSE") || !input.readUTF().equals("FLOAT32")) throw new IOException("Expected dense FLOAT32 weights");
            int rank = input.readInt();
            if (rank < 1 || rank > 2) throw new IOException("Unsupported tensor rank");
            long[] shape = new long[rank];
            int elements = 1;
            for (int i = 0; i < rank; i++) {
                shape[i] = input.readLong();
                if (shape[i] < 1 || shape[i] > 128) throw new IOException("Invalid combat tensor dimensions");
                elements = Math.multiplyExact(elements, (int) shape[i]);
            }
            int layout = input.readInt();
            if (layout < 0 || layout > rank) throw new IOException("Invalid layout");
            for (int i = 0; i < layout; i++) input.readChar();
            int endian = input.readUnsignedByte();
            if (endian != '<' && endian != '>') throw new IOException("Invalid tensor byte order");
            if (input.readInt() != elements * Float.BYTES) throw new IOException("Invalid tensor length");
            byte[] data = input.readNBytes(elements * Float.BYTES);
            if (data.length != elements * Float.BYTES) throw new EOFException("Truncated tensor");
            ByteBuffer buffer = ByteBuffer.wrap(data).order(endian == '<' ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
            float[] values = new float[elements];
            for (int i = 0; i < elements; i++) {
                values[i] = buffer.getFloat();
                if (!Float.isFinite(values[i])) throw new IOException("Non-finite model weight");
            }
            tensors.add(new Tensor(name, shape, values));
            offset = bytes.length - remaining.available() - 1;
        }
        if (tensors.size() != 20) throw new IOException("Expected four linear layers and three batch normalization layers");
        int index = 0;
        for (int layer = 0; layer < 4; layer++) {
            checkShape(tensors.get(index++), "weight", WIDTHS[layer + 1], WIDTHS[layer]);
            checkShape(tensors.get(index++), "bias", WIDTHS[layer + 1]);
            if (layer < 3) for (String name : List.of("gamma", "beta", "runningMean", "runningVar")) checkShape(tensors.get(index++), name, WIDTHS[layer + 1]);
        }
        return new KillAuraModelTodoAi(tensors);
    }
    private static void checkShape(Tensor tensor, String name, long... shape) throws IOException {
        if (!tensor.name.equals(name) || !Arrays.equals(tensor.shape, shape)) throw new IOException("Incompatible combat model architecture");
        if (name.equals("runningVar")) for (float value : tensor.values) if (value < 0) throw new IOException("Negative model variance");
    }
    public static KillAuraModelTodoAi load(String name, Path customFolder) throws IOException {
        if (name == null || !name.matches("[A-Za-z0-9_-]{1,64}")) throw new IOException("Use a model name containing letters, numbers, underscores or dashes");
        Path custom = customFolder.resolve(name + "TodoAi.params");
        if (Files.isRegularFile(custom)) try (InputStream input = Files.newInputStream(custom)) { return load(input); }
        // Preserve compatibility with user model directories from the previous port.
        Path legacy = customFolder.resolve(name);
        if (Files.isDirectory(legacy)) try (var files = Files.list(legacy)) {
            Path params = files.filter(p -> p.getFileName().toString().matches("tf-\\d+\\.params")).sorted().reduce((a, b) -> b).orElse(null);
            if (params != null) try (InputStream input = Files.newInputStream(params)) { return load(input); }
        }
        try (InputStream input = KillAuraModelTodoAi.class.getResourceAsStream("/assets/camwenmod/models/" + name.toLowerCase(Locale.ROOT) + "TodoAi.params")) { return load(input); }
    }
    public float[] predict(float[] input) {
        if (input.length != 6) throw new IllegalArgumentException("Expected six combat features");
        for (float value : input) if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite combat feature");
        float[] current = input.clone();
        int index = 0;
        for (int layer = 0; layer < 4; layer++) {
            float[] weight = parameters.get(index++).values, bias = parameters.get(index++).values;
            float[] next = new float[WIDTHS[layer + 1]];
            for (int row = 0; row < next.length; row++) {
                float sum = 0;
                for (int col = 0; col < current.length; col++) sum += weight[row * current.length + col] * current[col];
                next[row] = sum + bias[row];
            }
            if (layer < 3) {
                float[] gamma = parameters.get(index++).values, beta = parameters.get(index++).values;
                float[] mean = parameters.get(index++).values, variance = parameters.get(index++).values;
                for (int i = 0; i < next.length; i++) next[i] = Math.max(0, (next[i] - mean[i]) / (float) Math.sqrt(variance[i] + 1E-5f) * gamma[i] + beta[i]);
            }
            current = next;
        }
        for (float value : current) if (!Float.isFinite(value)) throw new IllegalStateException("Model output is non-finite");
        return current;
    }
}
