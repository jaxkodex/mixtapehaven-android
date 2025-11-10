## AI Design Prompt: Mixtape Haven - Server Connection / Onboarding Screen

**Objective:** Design the initial "Server Connection / Onboarding Screen" for the "Mixtape Haven" mobile app, strictly adhering to the **"Seamless Hyper-Efficiency"** design philosophy and the established Modern Futuristic color and typography system. The output must be a sleek, high-contrast console interface that prioritizes **speed, clarity, and efficiency**.

### I. Overarching Design Rationale and Aesthetics

1.  **Design Philosophy:** **"Seamless Hyper-Efficiency"**. The user should feel as if they are interacting with a personalized, **high-performance data console**, providing a **private and uninterrupted listening experience**.
2.  **Key Design Goal:** Utilize the ultra-dark background to make content elements appear to **glow or float**, ensuring **modern readability and professionalism**.

### II. Color Palette System

The design must use maximal contrast based on the mandatory **AMOLED black theme**:

| Role | Specific Color Name | Hex Code | Application |
| :--- | :--- | :--- | :--- |
| **Primary Background** | **Deep Space Black** | **#000000** | Full screen background. Addresses concerns regarding "battery drain". |
| **Primary Content/Text** | **Lunar White** | **#F0F0F0** | Majority of body text, input field labels, and essential functional text. Ensures **clean, highly legible** contrast. |
| **Accent Color (Primary CTAs)** | **Cyber Neon Blue** | **#00FFFF** | Used for the main interactive element ("Connect" button), input field focus indicators, and key navigation highlights. |
| **Accent Color (Secondary/Highlight)** | **Vaporwave Magenta** | **#FF00FF** | Reserved for subtle branding elements, error feedback, or system status alerts. |
| **Surface/Divider Color** | **Gunmetal Gray** | **#333333** | Subtle use for input field borders, separators, or card surfaces.

### III. Typography and Visual Clarity

1.  **Primary/Body Font:** A **clean, highly legible sans-serif font** (geometric or hyper-minimalist weight) must be used for all input fields, labels, and instructional text. This supports **efficiency, smoothness, and visual clarity** and is crucial for clear, logical navigation.
2.  **Secondary/Accent Font:** A **monospaced or segmented display font** should be used sparingly for key titles or status indicators (e.g., a "System Status: Online" label) to subtly inject the feeling of **digital data streams or console readouts**.

### IV. Spacing and Layout Requirements

The layout must be **clean and uncluttered**, explicitly avoiding **"wasted white space" or "oversized elements"**.

*   **Efficiency:** Related input fields must be grouped tightly to support the aesthetic of **condensed but readable lists**.
*   **Clarity:** Adequate, but not excessive, vertical spacing should separate the core input block, the main action button, and the supplementary help/privacy messages to maintain **visual clarity**.
*   **Interactivity:** Ensure interactive elements (input fields, buttons) have sufficient touch target areas, even if their visual footprint adheres to **minimalist icons**.

### V. Page Definition: Server Connection / Onboarding Screen

**User Intention:** The user's primary goal is to **establish a verifiable connection to their self-hosted Jellyfin server**, which is the foundational first step for using the application.

**Expected Actions and Element Design:**

1.  **Branding & Privacy:**
    *   Subtly integrate the "Mixtape Haven" brand identity and logo.
    *   Include a subtle, reassuring message below the inputs regarding data handling (e.g., focusing on the **ad-free and private experience** and **total control over files and data**). This should not clutter the main input area.

2.  **Input Fields (Core Functionality):**
    *   **Jellyfin Server URL:** (e.g., `http://192.168.0.200:8096`).
    *   **Username**.
    *   **Password**.
    *   **UI Interaction:** Use **Lunar White** text and clear, concise labels/placeholder text. When the field gains focus, its surrounding boundary or underline should illuminate with a **Cyber Neon Blue** glow to signify activity.

3.  **Action Button (Primary CTA):**
    *   **Label:** "Connect" or "Sign In".
    *   **Design:** This must be the most visually prominent element. It should utilize the **Cyber Neon Blue** accent color to initiate the authentication process with the Jellyfin API.
    *   **Feedback:** Must incorporate **fluid animations** upon press. During processing, implement an appropriate loading indicator (ideally a minimalist, futuristic spinner or data stream indicator) to inform the user that the app is processing the request.

4.  **Troubleshooting and Feedback:**
    *   **Troubleshooting Option:** Include a visible, easily accessible "Troubleshoot Connection" or "Help" link, crucial for ensuring a **smooth onboarding experience for non-technical users**. This link should be styled in **Cyber Neon Blue**.
    *   **Error Feedback:** Provide **clear, concise feedback** for connection errors ("Server unreachable," authentication failures). This temporary message should be highly visible, potentially utilizing the **Vaporwave Magenta** color.