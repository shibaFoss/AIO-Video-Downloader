package lib.process;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * Utility class for executing asynchronous jobs in the background with optional result delivery
 * and progress updates on the main thread.
 *
 * @param <TaskResult> The type of result returned by the background task.
 */
public final class AsyncJobUtils<TaskResult> {

    // Handler for posting tasks to the main UI thread
    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    // Logger utility (for debugging/logging purposes)
    private final LogHelperUtils logger = LogHelperUtils.from(getClass());

    // Core components for async execution
    private BackgroundTask<TaskResult> backgroundTask;
    private ResultTask<TaskResult> resultTask;
    private ProgressUpdateTask progressUpdateTask;
    private ExecutorService executorService;
    private Thread backgroundThread;
    private FutureTask<?> backgroundFutureTask;
    private TaskResult result;

    /**
     * Executes a UI task on the main thread.
     *
     * @param uiTask The task to be executed on the UI thread.
     */
    public static void executeOnMainThread(final @NonNull UITask uiTask) {
        UI_HANDLER.post(uiTask::runOnUIThread);
    }

    /**
     * Executes a background task without expecting any result.
     *
     * @param backgroundTask The task to be executed in the background.
     */
    public static void executeInBackground(final @NonNull BackgroundTaskNoResult backgroundTask) {
        new Thread(backgroundTask::runInBackground).start();
    }

    /**
     * Executes a background task using the provided executor service.
     *
     * @param backgroundTask The background task to execute.
     * @param executor       The executor service for managing background threads.
     * @return A FutureTask representing the asynchronous operation.
     */
    @NonNull
    public static FutureTask<?> executeInBackground(
            final @NonNull BackgroundTaskNoResult backgroundTask,
            @NonNull ExecutorService executor) {
        FutureTask<?> task = new FutureTask<>(backgroundTask::runInBackground, null);
        executor.submit(task);
        return task;
    }

    /**
     * Starts the background task and handles result delivery and progress updates.
     */
    public void start() {
        if (backgroundTask != null) {
            Runnable task = () -> {
                result = backgroundTask.runInBackground(progress -> {
                    if (progressUpdateTask != null) {
                        UI_HANDLER.post(() -> progressUpdateTask.onProgressUpdate(progress));
                    }
                });
                deliverResult();
            };

            if (executorService != null) {
                backgroundFutureTask = new FutureTask<>(task, null);
                executorService.submit(backgroundFutureTask);
            } else {
                backgroundThread = new Thread(task);
                backgroundThread.start();
            }
        }
    }

    /**
     * Delivers the result of the background task to the result handler on the main thread.
     */
    private void deliverResult() {
        if (resultTask != null) {
            UI_HANDLER.post(() -> resultTask.onResult(result));
        }
    }

    /**
     * Cancels the running background task.
     */
    public void cancel() {
        if (executorService != null && backgroundFutureTask != null) {
            backgroundFutureTask.cancel(true);
        } else if (backgroundThread != null) {
            backgroundThread.interrupt();
        }
    }

    // ---------------------------- Getters & Setters ----------------------------

    @NonNull
    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(@NonNull ExecutorService executorService) {
        this.executorService = executorService;
    }

    @NonNull
    public BackgroundTask<TaskResult> getBackgroundTask() {
        return backgroundTask;
    }

    public void setBackgroundTask(@NonNull BackgroundTask<TaskResult> backgroundTask) {
        this.backgroundTask = backgroundTask;
    }

    @NonNull
    public ResultTask<TaskResult> getResultTask() {
        return resultTask;
    }

    public void setResultTask(@NonNull ResultTask<TaskResult> resultTask) {
        this.resultTask = resultTask;
    }

    @NonNull
    public ProgressUpdateTask getProgressUpdateTask() {
        return progressUpdateTask;
    }

    public void setProgressUpdateTask(@NonNull ProgressUpdateTask progressUpdateTask) {
        this.progressUpdateTask = progressUpdateTask;
    }

    // ---------------------------- Interfaces ----------------------------

    /**
     * Represents a background task with progress updates and a result.
     *
     * @param <Result> The type of result produced by the background task.
     */
    public interface BackgroundTask<Result> {
        Result runInBackground(@NonNull ProgressCallback progressCallback);
    }

    /**
     * Represents a task that handles the result of a background operation.
     *
     * @param <Result> The type of result to handle.
     */
    public interface ResultTask<Result> {
        void onResult(Result result);
    }

    /**
     * Represents a task that should be run on the UI thread.
     */
    public interface UITask {
        void runOnUIThread();
    }

    /**
     * Represents a background task that does not return any result.
     */
    public interface BackgroundTaskNoResult {
        void runInBackground();
    }

    /**
     * Represents a task to handle progress updates.
     */
    public interface ProgressUpdateTask {
        void onProgressUpdate(int progress);
    }

    /**
     * Callback interface for reporting progress from background tasks.
     */
    public interface ProgressCallback {
        void onProgress(int progress);
    }

    // ---------------------------- Builder Class ----------------------------

    /**
     * Builder class for constructing instances of {@link AsyncJobUtils} with custom tasks.
     *
     * @param <JobResult> The type of result produced by the background task.
     */
    public static class Builder<JobResult> {

        private BackgroundTask<JobResult> backgroundTask;
        private ResultTask<JobResult> resultTask;
        private ProgressUpdateTask progressUpdateTask;
        private ExecutorService executorService;

        /**
         * Sets the background task to be executed.
         *
         * @param task The background task.
         * @return The builder instance.
         */
        @NonNull
        public Builder<JobResult> withBackgroundTask(@NonNull BackgroundTask<JobResult> task) {
            this.backgroundTask = task;
            return this;
        }

        /**
         * Sets the result task to handle the background task's output.
         *
         * @param task The result handler task.
         * @return The builder instance.
         */
        @NonNull
        public Builder<JobResult> withResultTask(@NonNull ResultTask<JobResult> task) {
            this.resultTask = task;
            return this;
        }

        /**
         * Sets the progress update task to handle progress changes.
         *
         * @param task The progress update handler.
         * @return The builder instance.
         */
        @NonNull
        public Builder<JobResult> withProgressUpdateTask(@NonNull ProgressUpdateTask task) {
            this.progressUpdateTask = task;
            return this;
        }

        /**
         * Sets the executor service for managing background tasks.
         *
         * @param executor The executor service.
         * @return The builder instance.
         */
        @NonNull
        public Builder<JobResult> withExecutorService(@NonNull ExecutorService executor) {
            this.executorService = executor;
            return this;
        }

        /**
         * Builds and returns an instance of {@link AsyncJobUtils} with the specified tasks.
         *
         * @return The constructed {@link AsyncJobUtils} instance.
         */
        @NonNull
        public AsyncJobUtils<JobResult> build() {
            AsyncJobUtils<JobResult> asyncJobUtils = new AsyncJobUtils<>();
            asyncJobUtils.setBackgroundTask(backgroundTask);
            asyncJobUtils.setResultTask(resultTask);
            asyncJobUtils.setProgressUpdateTask(progressUpdateTask);
            asyncJobUtils.setExecutorService(executorService);
            return asyncJobUtils;
        }
    }
}
