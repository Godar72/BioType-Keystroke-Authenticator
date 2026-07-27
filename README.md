# BioType — Keystroke Dynamics Biometric Authentication System

> **Version 2.0.0** | Phase 2 Production Release | Build 2026-04-01

A pure-Java keystroke biometric authentication system that identifies users by **how they type**, not what they type. The system captures key hold duration and inter-key flight time to build unique typing profiles and verify identity using Euclidean distance comparison.

---

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Setup & Installation](#setup--installation)
- [Usage Guide](#usage-guide)
- [Admin Guide](#admin-guide)
- [Testing](#testing)
- [File Structure](#file-structure)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)

---

## Features

### Core Authentication
- **Keystroke capture** using `System.nanoTime()` for nanosecond-precision timing
- **Euclidean distance scoring** converting timing vectors to 0-100% similarity
- **Adaptive threshold** (40-80% range) that adjusts based on authentication outcomes
- **Impostor detection** with bot-check, speed anomaly, rhythm analysis, and session lockout

### Security
- **Role-based access**: Admin (password) vs User (biometric)
- **Input validation** against injection, path traversal, and invalid data
- **Session lockout** after 3 consecutive failed attempts
- **Comprehensive audit logging** with daily rotation

### Analytics & Management
- **Profile quality analysis** — typing speed, rhythm consistency, quality score
- **Security analytics** — accuracy rates, weak profiles, anomaly detection
- **System backup/restore** — ZIP-compressed archives of all data
- **Configuration management** — properties-file based settings

### Testing & Demo
- **30+ unit tests** covering all components with PASS/FAIL reporting
- **Performance benchmarks** — auth speed, 50-user stress test, memory profiling
- **Automated demo script** — 7-step showcase of all features with synthetic data
- **Demo data generator** — 10 users with distinct typing personalities

---

## Architecture

```
com.keystroke.auth/
├── KeystrokeAuthSystem.java    ← Main entry point
├── User.java (abstract)        ← Base class
│   ├── Admin.java              ← Password-based admin
│   └── RegularUser.java        ← Biometric-authenticated user
├── KeystrokeCapture.java       ← Timing capture engine
├── KeystrokeProfile.java       ← Profile data model
├── FileManager.java            ← Persistence layer
├── SimilarityScorer.java       ← Strategy interface
│   └── EuclideanSimilarityScorer.java ← Distance algorithm
├── AuthenticationEngine.java   ← Central orchestrator
├── ThresholdManager.java       ← Adaptive threshold
├── ImpostorDetector.java       ← Anomaly detection
├── AuthLogger.java             ← Audit trail
├── AuthResult.java             ← Immutable result
├── ProfileAnalyzer.java        ← Quality analysis
├── AnalyticsEngine.java        ← System metrics
├── InputValidator.java         ← Input security
├── ConfigManager.java          ← Settings management
├── MenuSystem.java             ← ANSI console UI
├── BackupManager.java          ← Backup/restore
├── DemoDataGenerator.java      ← Synthetic data
├── DemoScript.java             ← Automated showcase
├── TestSuite.java              ← Unit tests
├── PerformanceTester.java      ← Benchmarks
├── SystemConstants.java        ← Centralized constants
├── AuthenticationException.java ← Auth errors
└── ProfileException.java       ← Profile errors
```

---

## Setup & Installation

### Prerequisites
- **Java JDK 8+** (any edition)
- No external libraries required — pure Java implementation

### Compile
```bash
cd Keystroke-Biometrics
javac -d out src/com/keystroke/auth/*.java
```

### Run
```bash
java -cp out com.keystroke.auth.KeystrokeAuthSystem
```

### Quick Start
1. **Compile** using the command above
2. **Run** the application
3. Select **Option 5 (Run Demo)** for an automated showcase
4. Or select **Option 1 (Enroll)** to register your typing profile

---

## Usage Guide

### Main Menu
```
1) Enroll New User         — Register by typing the phrase 3 times
2) User Login              — Authenticate via keystroke biometrics
3) Admin Login             — Access admin functions (admin/admin123)
4) View Saved Profiles     — Browse enrolled profiles with analysis
5) Run Demo                — Automated demo with synthetic data
6) Run Tests               — Unit tests and performance benchmarks
7) Help                    — Detailed help screen
8) Exit                    — Save config and exit
```

### Enrollment Process
1. Choose option 1 from the main menu
2. Enter a username (3-20 chars, alphanumeric + underscores)
3. Type the standard phrase **3 times** when prompted
4. System builds your biometric profile from averaged timing data
5. Profile quality analysis runs automatically

### Authentication Process
1. Choose option 2 from the main menu
2. Enter your enrolled username
3. Type the standard phrase **once**
4. System compares your typing pattern against stored profile
5. Result shows: confidence %, threshold, impostor risk, PASS/FAIL

---

## Admin Guide

**Login**: Username `admin`, Password `admin123`

### Admin Menu Options
| # | Function | Description |
|---|----------|-------------|
| 1 | View All Users | List all enrolled profiles |
| 2 | Delete User | Remove a user's profile (with confirmation) |
| 3 | View Auth Logs | Display today's authentication log entries |
| 4 | Adjust Threshold | Set a new threshold (40-80%) or reset to default |
| 5 | Daily Report | Auth attempts, success rate, per-user breakdown |
| 6 | Threshold Status | Current threshold, adjustments, allowed range |
| 7 | Security Analytics | Full security report with anomaly detection |
| 8 | Configuration | View/reset system settings |
| 9 | Create Backup | ZIP backup of all profiles, logs, settings |
| 10 | Restore Backup | Restore system from a backup archive |
| 11 | Logout | Return to main menu |

---

## Testing

### Run Unit Tests
Select menu option **6 → 1** to run the test suite.

Test coverage:
- **Keystroke Capture** — mean/stddev calculations, edge cases
- **Profile Building** — 1, 3, and 5 sample construction
- **Euclidean Distance** — identical, similar, different profile pairs
- **Threshold Adjustment** — false-reject, bounds clamping, reset
- **File Operations** — save, load, delete, missing files
- **Authentication Engine** — genuine users, impostors, error codes
- **Input Validator** — username rules, timing bounds, sanitization
- **Impostor Detector** — normal, bot-like, session locking

### Run Performance Benchmarks
Select menu option **6 → 2** for benchmarks:
- **Auth Speed** — 20-iteration average (target: <5ms)
- **Stress Test** — 50 concurrent profiles (create, load, auth)
- **Memory Usage** — per-profile memory footprint

---

## File Structure

### Runtime Data (auto-created)
```
profiles/
├── users/          ← .profile files (CSV-formatted timing data)
├── thresholds/     ← system_threshold.txt
├── logs/           ← auth_YYYY_MM_DD.txt daily logs
├── admin/          ← config.properties
└── backups/        ← keystroke_backup_YYYY_MM_DD.zip
```

### Source Code
```
src/com/keystroke/auth/    ← 27 Java source files
out/com/keystroke/auth/    ← 30 compiled .class files
```

---

## Configuration

Settings are stored in `profiles/admin/config.properties`:

| Key | Default | Description |
|-----|---------|-------------|
| `default_threshold` | 60.0 | Authentication threshold (%) |
| `enrollment_samples` | 3 | Typing samples per enrollment |
| `max_failed_attempts` | 3 | Failures before session lock |
| `session_timeout_minutes` | 30 | Session timeout |
| `hold_weight` | 0.6 | Weight for hold timing scoring |
| `flight_weight` | 0.4 | Weight for flight timing scoring |
| `impostor_flag_threshold` | 70.0 | Risk score to flag suspicious |
| `log_retention_days` | 90 | Days to keep log files |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `No profile found` | Enroll the user first (Option 1) |
| `Session locked` | Wait or ask admin to reset (Admin → threshold menu) |
| `Low confidence scores` | Re-enroll with consistent typing (Option 2 → Re-enroll) |
| `Compile errors` | Ensure JDK 8+ and compile all files: `javac -d out src/com/keystroke/auth/*.java` |
| `profiles/ not created` | Directory is auto-created on first run |

---

## OOP Design Patterns

| Pattern | Implementation |
|---------|---------------|
| **Inheritance** | `User` → `Admin`, `RegularUser` |
| **Polymorphism** | `login()`, `displayDashboard()` per role |
| **Strategy** | `SimilarityScorer` → `EuclideanSimilarityScorer` |
| **Facade** | `AuthenticationEngine` wraps scorer + threshold + detector + logger |
| **Immutable Object** | `AuthResult` (all fields final) |
| **Exception Hierarchy** | `AuthenticationException`, `ProfileException` with error code enums |

---

*BioType v2.0.0 — Keystroke Dynamics Biometric Authentication*
