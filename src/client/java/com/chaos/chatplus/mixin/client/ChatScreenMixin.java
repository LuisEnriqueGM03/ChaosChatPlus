package com.chaos.chatplus.mixin.client;
import com.mojang.blaze3d.platform.InputConstants;

import java.nio.file.Path;
import java.util.List;
import com.chaos.chatplus.client.*;
import com.chaos.chatplus.client.ui.*;
import com.chaos.chatplus.client.screen.GifViewerScreen;
import com.chaos.chatplus.gif.GifInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen implements SelfTest.BarAccess {
    @Shadow protected EditBox input;
    @Shadow private boolean handleComponentClicked(Style style, boolean insertion) { throw new AssertionError(); }
    @Unique private ChatBar chaoschatplus$bar;
    @Unique private int chaoschatplus$mx, chaoschatplus$my;
    protected ChatScreenMixin(Component title) { super(title); }
    public ChatBar chaoschatplus$bar() { return chaoschatplus$bar; }
    @Inject(method="init", at=@At("TAIL"))
    private void chaoschatplus$init(CallbackInfo ci) {
        chaoschatplus$bar = ModernChat.enabled() ? new ChatBar((ChatScreen)(Object)this, input) : null;
        if (chaoschatplus$bar != null) chaoschatplus$bar.onEdited();
    }
    @Inject(method="onEdited", at=@At("TAIL"))
    private void chaoschatplus$edited(String value, CallbackInfo ci) { if (chaoschatplus$bar != null) chaoschatplus$bar.onEdited(); }
    @Inject(method="extractRenderState", at=@At("HEAD"))
    private void chaoschatplus$mouse(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci) { chaoschatplus$mx=mx; chaoschatplus$my=my; }
    @WrapOperation(method="extractRenderState", at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private void chaoschatplus$bar(GuiGraphicsExtractor g,int x0,int y0,int x1,int y1,int color,Operation<Void> original) {
        if(chaoschatplus$bar!=null) chaoschatplus$bar.renderBar(g,chaoschatplus$mx,chaoschatplus$my); else original.call(g,x0,y0,x1,y1,color);
    }
    @Inject(method="extractRenderState", at=@At("TAIL"))
    private void chaoschatplus$overlays(GuiGraphicsExtractor g,int mx,int my,float partial,CallbackInfo ci) {
        if(chaoschatplus$bar!=null) {
            if(!chaoschatplus$bar.blocksMouse(mx,my)) {
                Style style = ModernChat.styleAt(mx,my);
                if (!ChatClicks.renderHover(g,style,mx,my) && style != null) {
                    if (style.getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowItem item)
                        g.setTooltipForNextFrame(font,item.item().create(),mx,my);
                    else if (style.getHoverEvent() instanceof net.minecraft.network.chat.HoverEvent.ShowText text)
                        g.setTooltipForNextFrame(font,text.value(),mx,my);
                }
            }
            chaoschatplus$bar.renderOverlays(g,mx,my);
        }
    }
    @Inject(method="keyPressed",at=@At("HEAD"),cancellable=true)
    private void chaoschatplus$keys(KeyEvent event,CallbackInfoReturnable<Boolean> cir) { if(chaoschatplus$bar!=null && chaoschatplus$bar.keyPressed(event.key())) cir.setReturnValue(true); }
    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void chaoschatplus$click(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir) {
        double mx=event.x(),my=event.y();
        if(chaoschatplus$bar!=null && chaoschatplus$bar.mouseClicked(mx,my,event.button())) { cir.setReturnValue(true); return; }
        if(!ModernChat.enabled() || event.button()!=InputConstants.MOUSE_BUTTON_LEFT) return;
        GifInfo gif=ModernChat.gifAt(mx,my);
        if(gif!=null) { Minecraft.getInstance().gui.setScreen(new GifViewerScreen(gif,(ChatScreen)(Object)this)); cir.setReturnValue(true); return; }
        Style style=ModernChat.styleAt(mx,my);
        if(style!=null && !event.hasShiftDown() && ChatClicks.handle(style,(ChatScreen)(Object)this)) cir.setReturnValue(true);
        else if(style!=null && handleComponentClicked(style,event.hasShiftDown())) cir.setReturnValue(true);
    }
    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void chaoschatplus$scroll(double mx,double my,double h,double v,CallbackInfoReturnable<Boolean> cir) { if(chaoschatplus$bar!=null && chaoschatplus$bar.mouseScrolled(mx,my,v)) cir.setReturnValue(true); }
    @ModifyVariable(method="handleChatInput",at=@At("HEAD"),argsOnly=true)
    private String chaoschatplus$codes(String message) {
        String text=EmojiInput.toCodes(message), target=ClientState.dmTarget();
        return target!=null && !text.isBlank() && !text.startsWith("/") ? "/msg "+target+" "+text : text;
    }
    @Inject(method="handleComponentClicked",at=@At("HEAD"),cancellable=true)
    private void chaoschatplus$specialClick(Style style,boolean insertion,CallbackInfoReturnable<Boolean> cir) {
        if(!insertion && ChatClicks.handle(style,(ChatScreen)(Object)this)) cir.setReturnValue(true);
    }
    @Override public boolean charTyped(CharacterEvent event) {
        if(chaoschatplus$bar!=null) {
            if(event.codepoint()<=Character.MAX_VALUE && chaoschatplus$bar.charTypedPicker((char)event.codepoint())) return true;
            chaoschatplus$bar.charTyped();
        }
        return super.charTyped(event);
    }
    @Override public void onFilesDrop(List<Path> paths) { ChatClicks.filesDropped(paths,(ChatScreen)(Object)this); }
}
