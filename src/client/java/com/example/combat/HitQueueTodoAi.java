package com.example.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** Client-thread FIFO; entity identity is retained, never re-resolved by name or network ID. */
public final class HitQueueTodoAi<T, O> {
    public enum Result { SENT, CANCELLED, EXPIRED, INVALID_TARGET, DISCONNECTED, DISABLED, FAILED }
    public enum Stage { QUEUED, AIMING, WAITING_FOR_CLICK, WAITING_FOR_CRITICAL, WAITING_FOR_ITEM,
        UNBLOCKING, ATTACKING, FINISHED }

    public static final class Request<T, O> {
        private final T target;
        private final O options;
        private final long deadline;
        private final CompletableFuture<Result> completion = new CompletableFuture<>();
        private Stage stage = Stage.QUEUED;

        private Request(T target, O options, long deadline) {
            this.target = target;
            this.options = options;
            this.deadline = deadline;
        }

        public T target() { return target; }
        public O options() { return options; }
        public Stage stage() { return stage; }
        public boolean isDone() { return completion.isDone(); }
        public CompletionStage<Result> completion() { return completion.minimalCompletionStage(); }
        /** Like scheduling, cancellation must run on the client thread. */
        public void cancel() { finish(Result.CANCELLED); }
        void stage(Stage value) { if (!isDone()) stage = value; }
        void finish(Result value) {
            stage = Stage.FINISHED;
            completion.complete(value);
        }
    }

    private final ArrayDeque<Request<T, O>> requests = new ArrayDeque<>();
    private final int capacity;

    public HitQueueTodoAi(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
    }

    public Request<T, O> schedule(T target, O options, long now, int timeoutTicks) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(options, "options");
        if (timeoutTicks < 1) throw new IllegalArgumentException("timeoutTicks");
        requests.removeIf(Request::isDone);
        if (requests.size() >= capacity) throw new IllegalStateException("Hit queue is full");
        var request = new Request<>(target, options, Math.addExact(now, timeoutTicks));
        requests.add(request);
        return request;
    }

    public Request<T, O> current(long now) {
        // Remove before completing so completion callbacks can safely enqueue another request.
        for (var request : new ArrayList<>(requests)) {
            if (request.isDone() || now >= request.deadline) {
                requests.remove(request);
                if (!request.isDone()) request.finish(Result.EXPIRED);
            }
        }
        return requests.peek();
    }

    public void finish(Request<T, O> request, Result result) {
        requests.remove(request);
        request.finish(result);
    }

    public void clear(Result result) {
        var pending = new ArrayDeque<>(requests);
        requests.clear();
        pending.forEach(request -> request.finish(result));
    }
}
