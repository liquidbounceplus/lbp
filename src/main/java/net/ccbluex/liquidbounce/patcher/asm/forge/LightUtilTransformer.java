/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.patcher.asm.forge;

import net.ccbluex.liquidbounce.patcher.tweaker.transform.PatcherTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class LightUtilTransformer implements PatcherTransformer {
    @Override
    public String[] getClassName() {
        return new String[]{"net.minecraftforge.client.model.pipeline.LightUtil"};
    }

    @Override
    public void transform(ClassNode classNode, String name) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("diffuseLight") && method.desc.equals("(FFF)F")) {
                clearInstructions(method);
                method.instructions.insert(fixLightingPipeline());
                break;
            }
        }
    }

    private InsnList fixLightingPipeline() {
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.FLOAD, 0));
        list.add(new VarInsnNode(Opcodes.FLOAD, 1));
        list.add(new VarInsnNode(Opcodes.FLOAD, 2));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, getHookClass("misc/LightUtilHook"), "diffuseLight", "(FFF)F", false));
        list.add(new InsnNode(Opcodes.FRETURN));
        return list;
    }
}