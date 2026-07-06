package com.xxsx.earthonline;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.Map;

import static java.util.Map.entry;

final class MaterialChemistry {
    private static final Map<String, Info> INFO = Map.ofEntries(
            entry("poor_magnetite_ore", info("Fe3O4 + SiO2 + Fe2O3", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("magnetite_ore", info("Fe3O4", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("rich_magnetite_ore", info("Fe3O4-rich", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("chalcopyrite_ore", info("CuFeS2 + FeS2", "tooltip.earth_online.chem.detail.sulfide_ore")),
            entry("auriferous_quartz_vein", info("SiO2 + Au", "tooltip.earth_online.chem.detail.vein")),
            entry("bituminous_coal_seam", info("C + volatiles + shale", "tooltip.earth_online.chem.detail.coal")),
            entry("anthracite_coal_seam", info("high-C coal + shale", "tooltip.earth_online.chem.detail.coal")),
            entry("kimberlite", info("olivine + serpentine + carbonate + mantle fragments", "tooltip.earth_online.chem.detail.mixed_rock")),
            entry("diamondiferous_kimberlite", info("kimberlite + C(diamond)", "tooltip.earth_online.chem.detail.mixed_rock")),
            entry("lapis_lazuli_ore", info("lazurite + calcite + pyrite", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("emerald_beryl_vein", info("Be3Al2Si6O18 + Cr/V trace", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("redstone_mineral_ore", info("Fe2O3 + HgS-like red minerals + MC redstone phase", "tooltip.earth_online.chem.detail.fantasy_mix")),
            entry("cinnabar_vein", info("HgS", "tooltip.earth_online.chem.detail.sulfide_ore")),
            entry("magnetite_chunk", info("Fe3O4", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("chalcopyrite_chunk", info("CuFeS2", "tooltip.earth_online.chem.detail.sulfide_ore")),
            entry("auriferous_quartz_chunk", info("SiO2 + Au", "tooltip.earth_online.chem.detail.vein")),
            entry("kimberlite_chunk", info("silicate-carbonate volcanic breccia", "tooltip.earth_online.chem.detail.mixed_rock")),
            entry("magnetite_dust", info("Fe3O4", "tooltip.earth_online.chem.detail.oxide")),
            entry("chalcopyrite_dust", info("CuFeS2", "tooltip.earth_online.chem.detail.sulfide")),
            entry("quartz_dust", info("SiO2", "tooltip.earth_online.chem.detail.silicate")),
            entry("feldspar_dust", info("KAlSi3O8 + NaAlSi3O8 + CaAl2Si2O8", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("mica_dust", info("K-Al-Mg-Fe sheet silicates", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("tailings_dust", info("SiO2 + silicates + residual oxides/sulfides", "tooltip.earth_online.chem.detail.tailings")),
            entry("coal_dust", info("C + volatile organics + ash", "tooltip.earth_online.chem.detail.coal")),
            entry("gold_dust", info("Au", "tooltip.earth_online.chem.detail.native_element")),
            entry("diamond_grit", info("C(diamond)", "tooltip.earth_online.chem.detail.native_element")),
            entry("cinnabar_chunk", info("HgS", "tooltip.earth_online.chem.detail.sulfide")),
            entry("cinnabar_dust", info("HgS", "tooltip.earth_online.chem.detail.sulfide")),
            entry("silica_dust", info("SiO2", "tooltip.earth_online.chem.detail.oxide")),
            entry("calcite_dust", info("CaCO3", "tooltip.earth_online.chem.detail.carbonate")),
            entry("lime_dust", info("CaO", "tooltip.earth_online.chem.detail.base")),
            entry("hematite_dust", info("Fe2O3", "tooltip.earth_online.chem.detail.oxide")),
            entry("pyrite_dust", info("FeS2", "tooltip.earth_online.chem.detail.sulfide")),
            entry("sulfur_dust", info("S", "tooltip.earth_online.chem.detail.native_element")),
            entry("mafic_silicate_dust", info("olivine + pyroxene + amphibole", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("aluminosilicate_dust", info("Al2O3 + SiO2 framework", "tooltip.earth_online.chem.detail.silicate_mix")),
            entry("iron_concentrate", info("Fe3O4/Fe2O3-rich concentrate", "tooltip.earth_online.chem.detail.concentrate")),
            entry("copper_concentrate", info("CuFeS2-rich concentrate", "tooltip.earth_online.chem.detail.concentrate")),
            entry("gold_concentrate", info("Au + SiO2 heavy fraction", "tooltip.earth_online.chem.detail.concentrate")),
            entry("roasted_copper_concentrate", info("Cu2S/CuO + Fe oxides", "tooltip.earth_online.chem.detail.roasted")),
            entry("redstone_concentrate", info("redstone phase + iron oxides", "tooltip.earth_online.chem.detail.fantasy_mix")),
            entry("lapis_concentrate", info("lazurite-rich concentrate", "tooltip.earth_online.chem.detail.concentrate")),
            entry("beryl_concentrate", info("Be3Al2Si6O18-rich concentrate", "tooltip.earth_online.chem.detail.concentrate")),
            entry("mercury_droplet", info("Hg", "tooltip.earth_online.chem.detail.native_element")),
            entry("slag", info("CaO-SiO2-Al2O3-FeO glass", "tooltip.earth_online.chem.detail.slag")),
            entry("salt_dust", info("NaCl", "tooltip.earth_online.chem.detail.salt")),
            entry("brine_crystal", info("NaCl + KCl + Mg/Ca salts", "tooltip.earth_online.chem.detail.salt_mix")),
            entry("sodium_hydroxide", info("NaOH", "tooltip.earth_online.chem.detail.base")),
            entry("chlorine_gas_cell", info("Cl2", "tooltip.earth_online.chem.detail.gas")),
            entry("hydrogen_gas_cell", info("H2", "tooltip.earth_online.chem.detail.gas")),
            entry("hydrochloric_acid", info("HCl(aq)", "tooltip.earth_online.chem.detail.acid")),
            entry("sulfuric_acid", info("H2SO4", "tooltip.earth_online.chem.detail.acid")),
            entry("nitric_acid", info("HNO3", "tooltip.earth_online.chem.detail.acid")),
            entry("ammonia", info("NH3", "tooltip.earth_online.chem.detail.base")),
            entry("ammonium_nitrate", info("NH4NO3", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("phosphate_rock_dust", info("Ca5(PO4)3(F,Cl,OH)", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("phosphoric_acid", info("H3PO4", "tooltip.earth_online.chem.detail.acid")),
            entry("fertilizer_blend", info("N-P-K salts + filler", "tooltip.earth_online.chem.detail.fertilizer_mix")),
            entry("soda_ash", info("Na2CO3", "tooltip.earth_online.chem.detail.salt")),
            entry("sodium_bicarbonate", info("NaHCO3", "tooltip.earth_online.chem.detail.salt")),
            entry("gypsum_dust", info("CaSO4.2H2O", "tooltip.earth_online.chem.detail.sulfate")),
            entry("cement_raw_meal", info("CaCO3 + SiO2 + Al2O3 + Fe2O3", "tooltip.earth_online.chem.detail.cement")),
            entry("cement_clinker", info("C3S + C2S + C3A + C4AF", "tooltip.earth_online.chem.detail.cement")),
            entry("cement_powder", info("clinker + gypsum", "tooltip.earth_online.chem.detail.cement")),
            entry("clay_dust", info("Al2Si2O5(OH)4 + silicates", "tooltip.earth_online.chem.detail.clay")),
            entry("bauxite_dust", info("AlOOH/Al(OH)3 + Fe oxides", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("aluminum_hydroxide", info("Al(OH)3", "tooltip.earth_online.chem.detail.base")),
            entry("alumina", info("Al2O3", "tooltip.earth_online.chem.detail.oxide")),
            entry("glass_batch", info("SiO2 + Na2CO3 + CaCO3", "tooltip.earth_online.chem.detail.glass")),
            entry("coke", info("C-rich coke + ash", "tooltip.earth_online.chem.detail.coal")),
            entry("coal_tar", info("aromatics + phenols + pitch", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("coal_gas_cell", info("H2 + CO + CH4 + CO2", "tooltip.earth_online.chem.detail.gas_mix")),
            entry("ethylene", info("C2H4", "tooltip.earth_online.chem.detail.monomer")),
            entry("polymer_resin", info("mixed polymer precursor", "tooltip.earth_online.chem.detail.polymer")),
            entry("calcium_chloride", info("CaCl2", "tooltip.earth_online.chem.detail.salt")),
            entry("slaked_lime", info("Ca(OH)2", "tooltip.earth_online.chem.detail.base")),
            entry("nitrogen_gas_cell", info("N2", "tooltip.earth_online.chem.detail.gas")),
            entry("oxygen_gas_cell", info("O2", "tooltip.earth_online.chem.detail.gas")),
            entry("carbon_dioxide_cell", info("CO2", "tooltip.earth_online.chem.detail.gas")),
            entry("sulfur_dioxide_cell", info("SO2", "tooltip.earth_online.chem.detail.gas")),
            entry("methanol", info("CH3OH", "tooltip.earth_online.chem.detail.solvent")),
            entry("formaldehyde", info("HCHO", "tooltip.earth_online.chem.detail.reactive_organic")),
            entry("urea", info("CO(NH2)2", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("potassium_chloride", info("KCl", "tooltip.earth_online.chem.detail.salt")),
            entry("potash", info("K2O equivalent", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("potassium_nitrate", info("KNO3", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("sodium_sulfate", info("Na2SO4", "tooltip.earth_online.chem.detail.sulfate")),
            entry("benzene", info("C6H6", "tooltip.earth_online.chem.detail.solvent")),
            entry("propylene", info("C3H6", "tooltip.earth_online.chem.detail.monomer")),
            entry("vinyl_chloride", info("C2H3Cl", "tooltip.earth_online.chem.detail.monomer")),
            entry("polyethylene_resin", info("(C2H4)n", "tooltip.earth_online.chem.detail.polymer")),
            entry("polypropylene_resin", info("(C3H6)n", "tooltip.earth_online.chem.detail.polymer")),
            entry("pvc_resin", info("(C2H3Cl)n", "tooltip.earth_online.chem.detail.polymer")),
            entry("steel_bloom", info("Fe + C", "tooltip.earth_online.chem.detail.alloy")),
            entry("ferrosilicon", info("Fe + Si", "tooltip.earth_online.chem.detail.alloy")),
            entry("aluminum_ingot", info("Al", "tooltip.earth_online.chem.detail.metal")),
            entry("crude_oil_sample", info("C5-C40 hydrocarbons + sulfur/nitrogen traces", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("natural_gas_cell", info("CH4-rich gas", "tooltip.earth_online.chem.detail.gas_mix")),
            entry("naphtha", info("C5-C10 hydrocarbons", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("kerosene_fraction", info("C10-C16 hydrocarbons", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("diesel_fraction", info("C12-C20 hydrocarbons", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("lubricating_oil", info("heavy hydrocarbons", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("asphalt", info("asphaltenes + resins + mineral filler", "tooltip.earth_online.chem.detail.petroleum_mix")),
            entry("petroleum_coke", info("C-rich coke", "tooltip.earth_online.chem.detail.coal")),
            entry("wood_chips", info("cellulose + hemicellulose + lignin", "tooltip.earth_online.chem.detail.biomass")),
            entry("cellulose_pulp", info("(C6H10O5)n + lignin traces", "tooltip.earth_online.chem.detail.biomass")),
            entry("bleached_pulp", info("(C6H10O5)n", "tooltip.earth_online.chem.detail.biomass")),
            entry("cellulose_fiber", info("(C6H10O5)n", "tooltip.earth_online.chem.detail.biomass")),
            entry("titanium_dioxide", info("TiO2", "tooltip.earth_online.chem.detail.oxide")),
            entry("iron_oxide_pigment", info("Fe2O3/FeOOH", "tooltip.earth_online.chem.detail.pigment")),
            entry("carbon_black", info("C", "tooltip.earth_online.chem.detail.pigment")),
            entry("paint_base", info("resin + pigment + solvent", "tooltip.earth_online.chem.detail.polymer_mix")),
            entry("graphite_dust", info("C(graphite)", "tooltip.earth_online.chem.detail.native_element")),
            entry("activated_carbon", info("porous C", "tooltip.earth_online.chem.detail.carbon_material")),
            entry("battery_carbon", info("conductive C", "tooltip.earth_online.chem.detail.carbon_material")),
            entry("manganese_oxide_dust", info("MnO2", "tooltip.earth_online.chem.detail.oxide")),
            entry("nickel_precursor", info("Ni(OH)2/Ni salts", "tooltip.earth_online.chem.detail.battery")),
            entry("lithium_salt", info("Li2CO3/Li salts", "tooltip.earth_online.chem.detail.battery")),
            entry("electrolyte", info("Li/K/Na salt + solvent", "tooltip.earth_online.chem.detail.battery")),
            entry("electrode_sheet", info("carbon + metal oxide + binder", "tooltip.earth_online.chem.detail.battery")),
            entry("simple_battery_cell", info("anode + cathode + electrolyte", "tooltip.earth_online.chem.detail.battery")),
            entry("hard_water_sample", info("H2O + Ca2+/Mg2+", "tooltip.earth_online.chem.detail.water")),
            entry("softened_water", info("H2O with reduced Ca/Mg", "tooltip.earth_online.chem.detail.water")),
            entry("humus_sample", info("humic organics + clay + microbes", "tooltip.earth_online.chem.detail.soil")),
            entry("sandy_loam_sample", info("sand + silt + clay + humus", "tooltip.earth_online.chem.detail.soil")),
            entry("alluvial_loam_sample", info("clay + silt + carbonates + humus", "tooltip.earth_online.chem.detail.soil")),
            entry("saline_soil_sample", info("soil minerals + NaCl/Na2CO3 salts", "tooltip.earth_online.chem.detail.soil")),
            entry("soil_mineral_mix", info("SiO2 + Al silicates + carbonates", "tooltip.earth_online.chem.detail.soil")),
            entry("irrigation_mineral_deposit", info("CaCO3 + CaSO4 + NaCl", "tooltip.earth_online.chem.detail.salt_mix")),
            entry("natural_latex", info("polyisoprene emulsion", "tooltip.earth_online.chem.detail.rubber")),
            entry("raw_rubber", info("(C5H8)n", "tooltip.earth_online.chem.detail.rubber")),
            entry("vulcanized_rubber", info("cross-linked polyisoprene + S", "tooltip.earth_online.chem.detail.rubber")),
            entry("rubber_compound", info("rubber + carbon black + sulfur + filler", "tooltip.earth_online.chem.detail.rubber")),
            entry("rubber_gasket", info("vulcanized rubber composite", "tooltip.earth_online.chem.detail.rubber")),
            entry("stainless_steel_bloom", info("Fe + Cr + Ni + C", "tooltip.earth_online.chem.detail.alloy")),
            entry("aluminum_alloy_billet", info("Al + Mg/Si/Cu", "tooltip.earth_online.chem.detail.alloy")),
            entry("copper_wire", info("Cu", "tooltip.earth_online.chem.detail.metal")),
            entry("thin_copper_power_cable", info("Cu conductor + glass insulation + redstone control", "tooltip.earth_online.chem.detail.cable")),
            entry("copper_power_cable", info("Cu conductor + glass insulation + redstone control", "tooltip.earth_online.chem.detail.cable")),
            entry("heavy_copper_power_cable", info("Cu conductor bundle + glass insulation + redstone control", "tooltip.earth_online.chem.detail.cable")),
            entry("fiberglass_cloth", info("SiO2-Al2O3-CaO glass fiber", "tooltip.earth_online.chem.detail.glass")),
            entry("copper_clad_laminate", info("fiberglass + epoxy + Cu foil", "tooltip.earth_online.chem.detail.electronics")),
            entry("printed_circuit_board", info("FR-4 + Cu traces", "tooltip.earth_online.chem.detail.electronics")),
            entry("solder_alloy", info("Sn + Pb/Ag/Cu", "tooltip.earth_online.chem.detail.alloy")),
            entry("ceramic_insulator", info("Al2O3/porcelain ceramic", "tooltip.earth_online.chem.detail.ceramic")),
            entry("sphalerite_dust", info("ZnS", "tooltip.earth_online.chem.detail.sulfide")),
            entry("zinc_oxide", info("ZnO", "tooltip.earth_online.chem.detail.oxide")),
            entry("zinc_ingot", info("Zn", "tooltip.earth_online.chem.detail.metal")),
            entry("galena_dust", info("PbS", "tooltip.earth_online.chem.detail.sulfide")),
            entry("lead_ingot", info("Pb", "tooltip.earth_online.chem.detail.metal")),
            entry("cassiterite_dust", info("SnO2", "tooltip.earth_online.chem.detail.oxide_ore")),
            entry("tin_ingot", info("Sn", "tooltip.earth_online.chem.detail.metal")),
            entry("kaolin_dust", info("Al2Si2O5(OH)4", "tooltip.earth_online.chem.detail.clay")),
            entry("firebrick", info("Al2O3-SiO2 refractory ceramic", "tooltip.earth_online.chem.detail.ceramic")),
            entry("mineral_wool", info("silicate glass fiber", "tooltip.earth_online.chem.detail.glass")),
            entry("ammonium_sulfate", info("(NH4)2SO4", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("single_superphosphate", info("Ca(H2PO4)2 + CaSO4", "tooltip.earth_online.chem.detail.fertilizer")),
            entry("carbon_monoxide_cell", info("CO", "tooltip.earth_online.chem.detail.gas")),
            entry("syngas_cell", info("CO + H2", "tooltip.earth_online.chem.detail.gas_mix"))
    );

    private MaterialChemistry() {
    }

    static void addDetails(ItemStack stack, java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!EarthOnline.MODID.equals(id.getNamespace())) {
            return;
        }
        Info info = INFO.get(id.getPath());
        if (info == null) {
            return;
        }
        if (!flag.hasShiftDown()) {
            lines.accept(Component.translatable("tooltip.earth_online.chem.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.accept(Component.translatable("tooltip.earth_online.chem.formula", info.formula()).withStyle(ChatFormatting.AQUA));
        lines.accept(Component.translatable(info.detailKey()).withStyle(ChatFormatting.GRAY));
    }

    private static Info info(String formula, String detailKey) {
        return new Info(formula, detailKey);
    }

    private record Info(String formula, String detailKey) {
    }
}
