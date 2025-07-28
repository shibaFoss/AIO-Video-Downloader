# Contributing

Thank you for considering contributing to **VideoMate** — a free, open-source, and ad-free video/audio downloader for Android, powered by [`yt-dlp`](https://github.com/yt-dlp/yt-dlp). This project aims to make downloading content from 1000+ websites easy and accessible through a clean Android interface.

---

## 🐞 Bug Reports

Before submitting a bug report:

- **Search existing issues**, including closed ones, to avoid duplicates.
- Use the **bug report template** and include clear, actionable details.

Please make sure to provide:

- Steps to reproduce the issue.
- The video/audio URL (if applicable).
- Your device model and Android version.
- Screenshots or logs, if possible.

Issues missing essential information may be closed without investigation.

---

## 💡 Feature Requests

VideoMate is designed to be a **simple and user-friendly interface** for yt-dlp, providing essential functionality without exposing the full command-line interface.

You're welcome to request features that improve usability, download flexibility, or integration with Android features.

> Note: Since VideoMate does not aim to support all yt-dlp CLI features, highly advanced options may not be considered.

---

## 📦 Pull Requests

We gladly welcome code contributions! Before submitting a pull request:

1. Comment under the relevant issue or open a new one to describe your changes.
2. Mention that you're working on it to avoid duplicate efforts.
3. Follow coding conventions and test your changes thoroughly.

Small, focused contributions are appreciated and easier to review.

---

## 👋 New Contributors

New to open source? Great!

- Look for issues labeled `good first issue`.
- Feel free to ask questions or request clarification.
- We’re happy to help you get started.

---

## ⚙️ Building From Source

To build and run the app locally on your development machine, follow the steps below. This guide assumes you have basic knowledge of Git and Android Studio. If you're new to either, feel free to ask for help in the community!

### 1. Fork the Repository

Create your own copy of the repository by clicking the **Fork** button on the top-right corner of the [VideoMate GitHub page](https://github.com/shibafoss/VideoMate). This allows you to freely experiment with changes without affecting the original project.

### 2. Clone the Repository

After forking, clone the repository to your local machine using the following Git command:

```bash
git clone https://github.com/shibafoss/VideoMate.git
```

Replace `shibafoss` with your GitHub username. This creates a local copy of the project on your machine.

### 3. Open the Project in Android Studio

- Launch **Android Studio**.
- Select **"Open an Existing Project"** from the welcome screen.
- Navigate to the folder where you cloned the repository and select it.

> ✅ It is recommended to use the latest **Android Studio Canary** version for the best compatibility with modern Android features and build tools.

### 4. Sync the Gradle Project

Once the project is open, Android Studio will attempt to sync all Gradle files automatically. If not prompted:

- Click **File > Sync Project with Gradle Files**.
- Make sure you have an active internet connection, as dependencies may need to be downloaded.

### 5. Resolve Dependencies (if needed)

If you run into missing dependencies or build errors:

- Make sure you’ve installed the required **Android SDKs** and **Build Tools**.
- Go to **Tools > SDK Manager** and install any missing components as prompted.

### 6. Build and Run the App

- Connect a physical Android device via USB (with USB debugging enabled) or use an Android emulator.
- Click the **Run ▶** button in the toolbar.
- Android Studio will compile the app and install it on the selected device/emulator.

### 7. Start Exploring and Contributing

Now that the app is running, you're all set to:

- Explore the source code.
- Make improvements or fix bugs.
- Test features.
- Submit pull requests!

> 💡 Tip: If you're unfamiliar with how to use Git branches, commits, or pull requests, many guides are available on GitHub Docs or reach out in the community chat.

---

## 📜 Contributor Guidelines

VideoMate is a project built with simplicity and clarity in mind. To maintain consistency across the codebase, please follow these contributor guidelines:

### 🔧 Coding Style

- Use **simple and clean Kotlin or Java code**. Avoid complex patterns unless absolutely necessary.
- Follow basic Android coding practices — stick to core SDK components (e.g., Activities, Services, BroadcastReceivers, etc.).
- Avoid frameworks or tools that add unnecessary abstraction or complexity, such as:
    - Dependency Injection (Dagger, Hilt)
    - Reactive frameworks (RxJava, Flow)
    - ViewModel/LiveData unless already used
    - Advanced architectural patterns (MVI, MVVM with complex toolchains)

### 🧩 Architecture

- Keep the architecture **minimal and intuitive**. There's no formal architecture enforced — just organized, modular, and readable code.
- Group related files together logically (e.g., UI, service, utils).
- Avoid over-abstracting features into multiple unnecessary layers.

### 📝 Code Practices

- Write methods that are short, self-explanatory, and focused on a single task.
- Use meaningful variable and method names.
- Add inline comments where logic may not be immediately clear.
- Avoid magic numbers or hardcoded strings — use constants where appropriate.
- Prefer readability and simplicity over clever or compact code tricks.

### 🚫 What Not to Use

- No third-party dependency injection libraries (like Dagger, Hilt).
- No complex build toolchains.
- Avoid introducing libraries that are not absolutely essential to the project.

### 🤝 Contribution Spirit

- This project is also intended for learning Android development, so keeping it simple helps everyone understand and contribute.
- Always aim for code that a beginner could read and learn from.

By contributing with these principles in mind, you help maintain a codebase that is beginner-friendly, stable, and easy to evolve over time.

## 🗨️ Community & Support

For questions, ideas, or support:

- **GitHub Issues**: [Open an issue](https://github.com/shibafoss/VideoMate/issues)
- **Email**: shiba.spj@hotmail.com
- **Telegram Group**: [Join our community](https://t.me/VideoMateApp)

---

## ⚠️ Disclaimer

This project is a personal learning initiative centered around Android development. While it is fully functional and continuously improved, it may not adhere to all industry best practices or architectural standards. Contributors and users are encouraged to approach with an open mind, and you're very welcome to contribute toward refining and enhancing it over time.