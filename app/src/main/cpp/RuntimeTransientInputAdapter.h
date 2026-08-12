#pragma once

#include "NDS.h"
#include "NDSCart.h"
#include "GBACart.h"

namespace MelonDSAndroid {

void applyRuntimeTransientInput(
    melonDS::GBACart::GBACartSlot& slot,
    melonDS::s16 axisXQ12,
    melonDS::s16 axisYQ12,
    melonDS::u16 scalar,
    melonDS::u16 actionSequence,
    melonDS::u16 flags);

}
