# Contributing

Thank you for considering contributing to **AIO Video Downloader** — a free, open-source, and
ad-free video/audio downloader for Android. It’s powered by the magic of [
`yt-dlp`](https://github.com/yt-dlp/yt-dlp) and brought to life on
mobile with the help of [youtubedl-android](https://github.com/yausername/youtubedl-android).

This project was born out of a simple frustration many of us share — you find the perfect video or
audio clip, but when you want to save it for offline use, the **`download`** button is mysteriously
missing (or buried under endless pop-ups). We decided to fix that.

With support for **1,000+ websites** and a clean, minimal Android
interface, [AIO Video Downloader](https://github.com/shibaFoss/AIO-Video-Downloader/)
aims to make grabbing your favorite content as easy as saving a photo from your gallery. Think of it
as your pocket-sized **`save button`** for the internet.

---

## 🐞 Bug Reports

Before reporting a bug:

- **Check existing issues** (open and closed) to avoid duplicates.
- Use the **bug report template** with clear, useful details.

Include:

- **What you did before the bug happened** — step-by-step so we can try it too.
- **The video or audio link** (if the bug happens with a specific file).
- **Your phone model and Android version** — e.g., "Samsung Galaxy A52, Android 13".
- **Pictures or error messages** — a screenshot or log helps us see what you see.

Without these details, it’s much harder to fix the problem, and your report might be closed.

---

## 💡 Feature Requests

**AIO Video Downloader** is designed to be a **simple and user-friendly interface** for
_[yt-dlp](https://github.com/yt-dlp/yt-dlp)_, providing essential functionality without exposing the
full command-line interface.

You're welcome to request features that improve usability, download flexibility, or integration with
Android features.

> **Note:** AIO Video Downloader focuses on being a simple, effective download solution — not a
> feature-packed toolbox. We intentionally avoid complex, advanced options from `yt-dlp` to keep the
> experience clean and confusion-free. The goal: you never have to think twice about how to use the
> app.

---

## 📦 Pull Requests

We’re happy to have your help! If you want to add or improve something:

1. **Talk to us first** — comment on an existing issue or open a new one to explain your idea.
2. **Say you’re working on it** so no one else does the same thing.
3. **Keep it clean and tested** — follow the project’s coding style and make sure your changes work
   well.

Small, focused changes are best — they’re easier to review and get merged faster.

---

## 👋 New Contributors

New to open source? Awesome — you’re in the right place!

- Start with issues marked **`good first issue`** — they’re beginner-friendly.
- Not sure about something? Just ask — no question is too small.
- We’re here to help you get started and feel confident contributing.

---

## ⚙️ Building From Source

This section is for anyone who wants to build AIO Video Downloader on their own computer and maybe
even make changes to it. If you’ve used **Git** and **Android Studio** before, you’ll feel right at
home. If not — no worries! You can still try, and you’re welcome to ask for help from the community.

### 1. Fork the Repository

Think of “forking” as making your own personal copy of the project on GitHub.  
Click the **Fork** button (top-right of
the [project page](https://github.com/shibafoss/AIO-Video-Downloader)), and now you can play around,
make edits, and test changes — all without touching the original version.

### 2. Clone the Repository

After forking, clone the repository to your local machine using the following Git command:

```bash
git clone https://github.com/shibafoss/AIO-Video-Downloader.git
```

Replace `shibafoss` with your GitHub username. This creates a local copy of the project on your
machine.

### 3. Open the Project in Android Studio

- Launch **Android Studio**.
- Select **"Open an Existing Project"** from the welcome screen.
- Navigate to the folder where you cloned the repository and select it.

> ✅ It is recommended to use the latest **Android Studio** version for the best compatibility
> with modern Android features and build tools.

### 4. Sync the Gradle Project

Once the project is open, Android Studio will attempt to sync all Gradle files automatically. If not
prompted:

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

> 💡 Tip: If you're unfamiliar with how to use Git branches, commits, or pull requests, many guides
> are available on GitHub Docs or reach out in the community chat.

---

## 📜 Contributor Guidelines

**AIO Video Downloader** is a project built with simplicity and clarity in mind. To maintain
consistency
across the codebase, please follow these contributor guidelines:

### 🔧 Coding Style

- Use **simple and clean Kotlin or Java code**. Avoid complex patterns unless absolutely necessary.
- Follow basic Android coding practices — stick to core SDK components (e.g., Activities, Services,
  BroadcastReceivers, etc.).
- Avoid frameworks or tools that add unnecessary abstraction or complexity, such as:
    - Dependency Injection (Dagger, Hilt)
    - Reactive frameworks (RxJava, Flow)
    - ViewModel/LiveData unless already used
    - Advanced architectural patterns (MVI, MVVM with complex toolchains)

### 🧩 Architecture

- Keep the architecture **minimal and intuitive**. There's no formal architecture enforced — just
  organized, modular, and readable code.
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

This is one of my personal passion projects, and part of the goal is to make it a space where anyone
can learn Android development. Keeping things simple isn’t just a choice — it’s how we make sure
everyone can follow along, understand, and contribute.

Please aim to write code that even a beginner could read and learn from. By contributing with this
spirit, you’re helping keep the project beginner-friendly, stable, and easy to grow over time — just
the way I dreamed it would be.

## 🗨️ Community & Support

For questions, ideas, or support:

- **GitHub Issues**: [Open an issue](https://github.com/shibafoss/AIO-Video-Downloader/issues)
- **Email**: shiba.spj@hotmail.com