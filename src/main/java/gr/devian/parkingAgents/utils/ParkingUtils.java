package gr.devian.parkingAgents.utils;

public final class ParkingUtils {
    private ParkingUtils() {

    }

    public static String getParkingSpotString(final int parkingSpot, final int spacesPerGroup) {
        final int group = parkingSpot / spacesPerGroup;
        final int space = parkingSpot % spacesPerGroup;
        return String.format("%c:%02d", (char) (65 + group), space);
    }

}
