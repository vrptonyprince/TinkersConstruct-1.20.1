package tconstruct.tools.client;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import java.lang.reflect.Field;
import java.util.List;

import tconstruct.TConstruct;
import tconstruct.tools.client.module.GuiModule;
import tconstruct.tools.inventory.ContainerMultiModule;
import tconstruct.tools.inventory.SlotWrapper;

public class GuiMultiModule extends GuiContainer {

  // NEI-stuff >:(
  private static Field NEI_Manager;

  static {
    try {
      NEI_Manager = GuiContainer.class.getDeclaredField("manager");
    } catch(NoSuchFieldException e) {
      NEI_Manager = null;
    }
  }

  protected List<GuiModule> modules = Lists.newArrayList();

  public int cornerX;
  public int cornerY;
  public int realWidth;
  public int realHeight;

  public GuiMultiModule(ContainerMultiModule container) {
    super(container);

    realWidth = -1;
    realHeight = -1;
  }

  protected void addModule(GuiModule module) {
    modules.add(module);
  }

  @Override
  public void initGui() {
    if(realWidth > -1) {
      // has to be reset before calling initGui so the position is getting retained
      xSize = realWidth;
      ySize = realHeight;
    }
    super.initGui();

    this.cornerX = this.guiLeft;
    this.cornerY = this.guiTop;
    this.realWidth = xSize;
    this.realHeight = ySize;

    for(GuiModule module : modules) {
      updateSubmodule(module);
    }

    //this.guiLeft = this.guiTop = 0;
    //this.xSize = width;
    //this.ySize = height;
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    for(GuiModule module : modules) {
      module.handleDrawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
    }
  }

  @Override
  public void setWorldAndResolution(Minecraft mc, int width, int height) {
    super.setWorldAndResolution(mc, width, height);

    // workaround for NEIs ASM hax. sigh.
    try {
      for(GuiModule module : modules) {
        module.setWorldAndResolution(mc, width, height);
        if(NEI_Manager != null) {
          NEI_Manager.set(module, NEI_Manager.get(this));
        }
        updateSubmodule(module);
      }
    } catch(IllegalAccessException e) {
      TConstruct.log.error(e);
    }
  }

  @Override
  public void onResize(Minecraft mc, int width, int height) {
    super.onResize(mc, width, height);

    for(GuiModule module : modules) {
      module.onResize(mc, width, height);
      updateSubmodule(module);
    }
  }

  @Override
  public void drawScreen(int mouseX, int mouseY, float partialTicks) {
    int oldX = guiLeft;
    int oldY = guiTop;
    int oldW = xSize;
    int oldH = ySize;

    guiLeft = cornerX;
    guiTop = cornerY;
    xSize = realWidth;
    ySize = realHeight;
    super.drawScreen(mouseX, mouseY, partialTicks);
    guiLeft = oldX;
    guiTop = oldY;
    xSize = oldW;
    ySize = oldH;
  }


  // needed to get the correct slot on clicking
  @Override
  protected boolean isPointInRegion(int left, int top, int right, int bottom, int pointX, int pointY) {
    pointX -= this.cornerX;
    pointY -= this.cornerY;
    return pointX >= left - 1 && pointX < left + right + 1 && pointY >= top - 1 && pointY < top + bottom + 1;
  }

  protected void updateSubmodule(GuiModule module) {
    module.updatePosition(this.cornerX, this.cornerY, this.realWidth, this.realHeight);

    if(module.guiLeft < this.guiLeft) {
      this.xSize += this.guiLeft - module.guiLeft;
      this.guiLeft = module.guiLeft;
    }
    if(module.guiTop < this.guiTop) {
      this.ySize += this.guiTop - module.guiTop;
      this.guiTop = module.guiTop;
    }
    if(module.guiRight() > this.guiLeft + this.xSize) {
      xSize = module.guiRight() - this.guiLeft;
    }
    if(module.guiBottom() > this.guiTop + this.ySize) {
      ySize = module.guiBottom() - this.guiTop;
    }
  }

  @Override
  public void drawSlot(Slot slotIn) {
    GuiModule module = getModuleForSlot(slotIn.slotNumber);

    if(module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if(slotIn instanceof SlotWrapper) {
        slot = ((SlotWrapper) slotIn).parent;
      }
      if(!module.shouldDrawSlot(slot)) {
        return;
      }
    }

    // update slot positions
    if(slotIn instanceof SlotWrapper) {
      slotIn.xDisplayPosition = ((SlotWrapper) slotIn).parent.xDisplayPosition;
      slotIn.yDisplayPosition = ((SlotWrapper) slotIn).parent.yDisplayPosition;
    }

    super.drawSlot(slotIn);
  }

  @Override
  public boolean isMouseOverSlot(Slot slotIn, int mouseX, int mouseY) {
    GuiModule module = getModuleForSlot(slotIn.slotNumber);

    // mouse inside the module of the slot?
    if(module != null) {
      Slot slot = slotIn;
      // unwrap for the call to the module
      if(slotIn instanceof SlotWrapper) {
        slot = ((SlotWrapper) slotIn).parent;
      }
      if(!module.shouldDrawSlot(slot)) {
        return false;
      }
    }

    return super.isMouseOverSlot(slotIn, mouseX, mouseY);
  }

  /*
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      GuiModule module = getModuleForPoint(mouseX, mouseY);
      if(module != null) {
        mouseX -= this.cornerX;
        mouseY -= this.cornerY;
        module.handleMouseClicked(mouseX, mouseY, mouseButton);
      }
      else {
        super.mouseClicked(mouseX, mouseY, mouseButton);
      }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
      GuiModule module = getModuleForPoint(mouseX, mouseY);
      if(module != null) {
        mouseX -= this.cornerX;
        mouseY -= this.cornerY;
        module.handleMouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
      }
      else {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
      }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
      GuiModule module = getModuleForPoint(mouseX, mouseY);
      if(module != null) {
        mouseX -= this.cornerX;
        mouseY -= this.cornerY;
        module.handleMouseReleased(mouseX, mouseY, state);
      }
      else {
        super.mouseReleased(mouseX, mouseY, state);
      }
    }

    private GuiModule getModuleForPoint(int x, int y) {
      for(GuiModule module : modules) {
        if(this.isPointInRegion(module.guiLeft, module.guiTop, module.guiRight(), module.guiBottom(), x + this.cornerX, y + this.cornerY)) {
          return module;
        }
      }

      return null;
    }
  */
  private GuiModule getModuleForSlot(int slotNumber) {
    return getModuleForContainer(getContainer().getSlotContainer(slotNumber));
  }

  private GuiModule getModuleForContainer(Container container) {
    for(GuiModule module : modules) {
      if(module.inventorySlots == container) {
        return module;
      }
    }

    return null;
  }

  private ContainerMultiModule getContainer() {
    return (ContainerMultiModule) inventorySlots;
  }

}
