package librarysystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads institution identity from the optional branding.properties resource. */
public final class LibraryBranding {
    private static final String DEFAULT_LIBRARY_NAME = "ARMY CENTRAL LIBRARY";
    private static final String DEFAULT_INSTITUTION_NAME = "RAWALPINDI, PAKISTAN";
    private static final String DEFAULT_APPLICATION_NAME =
            "Army Central Library Management System";
    private static final String DEFAULT_SUBTITLE = "Library Management & Book Administration";
    private static final String DEFAULT_SHORT_NAME = "ACL";

    private final String libraryName;
    private final String institutionName;
    private final String applicationName;
    private final String subtitle;
    private final String shortName;

    private LibraryBranding(String libraryName, String institutionName,
                            String applicationName, String subtitle, String shortName) {
        this.libraryName = libraryName;
        this.institutionName = institutionName;
        this.applicationName = applicationName;
        this.subtitle = subtitle;
        this.shortName = shortName;
    }

    /** Loads configured identity, falling back to neutral temporary labels. */
    public static LibraryBranding load() {
        Properties properties = new Properties();
        try (InputStream input = LibraryBranding.class.getResourceAsStream("/branding.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read branding.properties.", exception);
        }
        return new LibraryBranding(
                properties.getProperty("library.name", DEFAULT_LIBRARY_NAME).trim(),
                properties.getProperty("institution.name", DEFAULT_INSTITUTION_NAME).trim(),
                properties.getProperty("application.name", DEFAULT_APPLICATION_NAME).trim(),
                properties.getProperty("application.subtitle", DEFAULT_SUBTITLE).trim(),
                properties.getProperty("brand.shortName", DEFAULT_SHORT_NAME).trim());
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLogoResource() {
        return "/branding/logo.png";
    }
}
