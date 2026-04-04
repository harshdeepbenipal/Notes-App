# Scribble – Cross-Device Note-Taking Application

## Overview

Scribble is an Android-based note-taking application designed to provide a consistent and efficient user experience across smartphones and tablets. It enables users to create, edit, and manage notes with rich text formatting and cloud synchronization using Firebase.

The application emphasizes usability, adaptive interface design, and performance, ensuring reliable access to notes across different devices and orientations.

---

## Features

* Create, edit, and delete notes
* Cloud synchronization using Firebase
* Email-based user authentication
* Rich text formatting:

  * Bold
  * Italic
  * Underline
  * Highlight
  * Bullet lists
* Undo and redo functionality
* Search notes by title or content
* Trash system for recovering or permanently deleting notes
* Light and dark mode support
* Adaptive layouts for smartphones and tablets
* Offline access with automatic synchronization when reconnected

---

## Technologies Used

* Language: Java
* Development Environment: Android Studio
* User Interface: XML, ConstraintLayout, RecyclerView
* Backend and Database: Firebase (Authentication and Firestore/Realtime Database)

---

## Usage

Users can register or log in using an email account to access their notes. Notes can be created and edited using the built-in text editor with formatting tools available through the interface. All changes are automatically synchronized with Firebase, ensuring that notes remain accessible across devices. Users can search for notes, delete them, and restore them from the trash when needed.

---

## Project Structure

* activities/ – Application screens (e.g., login, main view, editor, trash)
* adapters/ – RecyclerView adapters for displaying notes
* models/ – Data models representing note objects
* utils/ – Helper classes for formatting and Firebase integration
* res/ – Layout files, themes, and other UI resources

---

## Limitations

* Limited to Android devices
* No real-time collaborative editing
* Minor layout issues on smaller screens in landscape mode
* Limited customization options

---

## Future Work

* Cross-platform support (e.g., Flutter or React Native)
* Real-time collaborative editing
* Improved layout handling for smaller devices
* Enhanced theme customization
* Support for multimedia content

---

## Contributors

* Harsh-Deep Benipal
* Prakriti Biswas
* Oshiya Brahmbhatt
* Cassandra Hutchinson

---

## License

This project was developed for academic purposes and is not intended for commercial use.
