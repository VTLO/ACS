/*
 * The FFmpegKit build we depend on statically links SDL2 into libavdevice.so but leaves
 * SDL's Android HIDAPI backend (PLATFORM_hid_*) unresolved, so the library fails to dlopen
 * ("cannot locate symbol PLATFORM_hid_write") and the app crashes on start. These stubs
 * report "no HID devices"; SDL then simply disables HIDAPI game controllers, which ffmpeg
 * never uses. app/build.gradle adds this library to libavdevice.so's DT_NEEDED entries.
 */
#include <stddef.h>
#include <wchar.h>

#define EXPORT __attribute__((visibility("default")))

typedef struct hid_device_ hid_device;
struct hid_device_info;

EXPORT int PLATFORM_hid_init(void) { return -1; }
EXPORT int PLATFORM_hid_exit(void) { return 0; }

EXPORT struct hid_device_info *PLATFORM_hid_enumerate(unsigned short vendor_id, unsigned short product_id) {
    (void) vendor_id; (void) product_id;
    return NULL;
}

EXPORT void PLATFORM_hid_free_enumeration(struct hid_device_info *devs) { (void) devs; }

EXPORT hid_device *PLATFORM_hid_open_path(const char *path, int exclusive) {
    (void) path; (void) exclusive;
    return NULL;
}

EXPORT void PLATFORM_hid_close(hid_device *dev) { (void) dev; }

EXPORT int PLATFORM_hid_write(hid_device *dev, const unsigned char *data, size_t length) {
    (void) dev; (void) data; (void) length;
    return -1;
}

EXPORT int PLATFORM_hid_read_timeout(hid_device *dev, unsigned char *data, size_t length, int ms) {
    (void) dev; (void) data; (void) length; (void) ms;
    return -1;
}

EXPORT int PLATFORM_hid_read(hid_device *dev, unsigned char *data, size_t length) {
    (void) dev; (void) data; (void) length;
    return -1;
}

EXPORT int PLATFORM_hid_set_nonblocking(hid_device *dev, int nonblock) {
    (void) dev; (void) nonblock;
    return -1;
}

EXPORT int PLATFORM_hid_send_feature_report(hid_device *dev, const unsigned char *data, size_t length) {
    (void) dev; (void) data; (void) length;
    return -1;
}

EXPORT int PLATFORM_hid_get_feature_report(hid_device *dev, unsigned char *data, size_t length) {
    (void) dev; (void) data; (void) length;
    return -1;
}

EXPORT int PLATFORM_hid_get_manufacturer_string(hid_device *dev, wchar_t *string, size_t maxlen) {
    (void) dev; (void) string; (void) maxlen;
    return -1;
}

EXPORT int PLATFORM_hid_get_product_string(hid_device *dev, wchar_t *string, size_t maxlen) {
    (void) dev; (void) string; (void) maxlen;
    return -1;
}

EXPORT int PLATFORM_hid_get_serial_number_string(hid_device *dev, wchar_t *string, size_t maxlen) {
    (void) dev; (void) string; (void) maxlen;
    return -1;
}

EXPORT int PLATFORM_hid_get_indexed_string(hid_device *dev, int index, wchar_t *string, size_t maxlen) {
    (void) dev; (void) index; (void) string; (void) maxlen;
    return -1;
}

EXPORT const wchar_t *PLATFORM_hid_error(hid_device *dev) {
    (void) dev;
    return NULL;
}
