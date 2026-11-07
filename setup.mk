# -------------------------------------------------------
# Wallpaper partner package.
# Paste into device/tecno/LH8n/device.mk
# -------------------------------------------------------

PRODUCT_PACKAGES += \
    CustomWallpapers

# LineageOS ships its own partner wallpaper package ("Backgrounds") and the wallpaper
# picker reads only ONE partner. This module's receiver uses priority=100 so it wins the
# resolution order, but if you want it to be the ONLY partner, uncomment the line below
# INSIDE lineage_LH8n.mk, AFTER:
#   $(call inherit-product, vendor/lineage/config/common_full_phone.mk)
#
# PRODUCT_PACKAGES := $(filter-out Backgrounds,$(PRODUCT_PACKAGES))
