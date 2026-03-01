(function () {
    var STORAGE_KEY = "smartqueue-theme";

    function getPreferredTheme() {
        var stored = localStorage.getItem(STORAGE_KEY);
        if (stored === "light" || stored === "dark") {
            return stored;
        }
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute("data-theme", theme);
        localStorage.setItem(STORAGE_KEY, theme);
        var label = document.querySelector(".theme-toggle-label");
        var icon = document.querySelector(".theme-icon");
        if (label) {
            label.textContent = theme === "dark" ? "Dark" : "Light";
        }
        if (icon) {
            icon.textContent = theme === "dark" ? "D" : "L";
        }
    }

    function buildToggle() {
        if (document.querySelector(".theme-toggle")) {
            return;
        }
        var btn = document.createElement("button");
        btn.className = "theme-toggle";
        btn.type = "button";
        btn.setAttribute("aria-label", "Toggle color theme");
        btn.innerHTML = '<span class="theme-icon"></span><span class="theme-toggle-label"></span>';
        btn.addEventListener("click", function () {
            var current = document.documentElement.getAttribute("data-theme") || "light";
            applyTheme(current === "dark" ? "light" : "dark");
        });
        document.body.appendChild(btn);
    }

    document.addEventListener("DOMContentLoaded", function () {
        applyTheme(getPreferredTheme());
        buildToggle();
    });
})();
