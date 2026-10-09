package com.example.killaura;
import com.example.aimassist.AimGeometryTodoAi;
import com.example.aimassist.AimGeometryTodoAi.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import static com.example.killaura.KillAuraGeometryTodoAi.*;
public final class KillAuraTestTodoAi {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        var cfg = new KillAuraConfigTodoAi();
        var box = new AABB(-0.3, 0, 2, 0.3, 1.8, 2.6);
        for (Vec3 eyes : new Vec3[]{new Vec3(0,1.62,0),new Vec3(4,4,4),new Vec3(0,8,2.3),new Vec3(0,1,2.3)}) {
            var points=projectedPoints(eyes,box);
            check(!points.isEmpty(), "projection has candidates from every viewing direction");
            for (var point:points) check(point.x>=box.minX-1e-7 && point.x<=box.maxX+1e-7 && point.y>=box.minY-1e-7 && point.y<=box.maxY+1e-7 && point.z>=box.minZ-1e-7 && point.z<=box.maxZ+1e-7, "projection stays on actual hitbox");
            check(choosePoint(eyes,box,cfg.aimPoint,p->false)==null,"occluded candidates cannot be selected");
        }
        cfg.aimPoint.exemptHead=true;
        var body=choosePoint(new Vec3(0,1.62,0),box,cfg.aimPoint,p->true);
        check(body.y<=1.2+1e-7,"head exemption chooses another body point");
        cfg.aimPoint.exemptBody=cfg.aimPoint.exemptFeet=true;
        check(choosePoint(Vec3.ZERO,box,cfg.aimPoint,p->true)!=null,"all exemptions fall back to a valid point");
        var current=new Rotation(179,20); var goal=new Rotation(-179,-20);
        for(var mode:KillAuraConfigTodoAi.Smoothing.values()) {
            cfg.rotations.smoothing=mode;
            cfg.rotations.horizontalMin=cfg.rotations.horizontalMax=15;
            cfg.rotations.verticalMin=cfg.rotations.verticalMax=15;
            Rotation next=smooth(current,goal,null,null,cfg.rotations);
            check(Double.isFinite(next.yaw())&&Double.isFinite(next.pitch()),"finite smoothing: "+mode);
            check(AimGeometryTodoAi.error(next,goal)<AimGeometryTodoAi.error(current,goal),"shortest wrapped turn approaches target: "+mode);
            check(next.pitch()>=goal.pitch()&&next.pitch()<=current.pitch(),"pitch cannot overshoot: "+mode);
            check(smooth(goal,goal,null,null,cfg.rotations).equals(goal),"settled rotation stays unchanged: "+mode);
        }
        var result=linear(new Rotation(0,0),new Rotation(90,0),20,20);
        check(result.yaw()==20&&result.pitch()==0,"linear limit applies in degrees per tick");
        check(finite(Double.NaN,3,0,5)==3,"invalid persisted number uses fallback");
        check(random(7,2,2,7)>=2,"reversed random bounds are safe");
        cfg.range=null;cfg.target=null;cfg.aimPoint=null;cfg.rotations=null;cfg.repair();
        check(cfg.range!=null&&cfg.target!=null&&cfg.aimPoint!=null&&cfg.rotations!=null,"old/null configs migrate safely");
        for (String bad : new String[]{"NaN", "Infinity", "-1", "6"}) {
            try {
                KillAuraScreenTodoAi.applyNumber(cfg.range,cfg.range.getClass().getField("rangeIncrease"),bad);
                throw new AssertionError("invalid aim range accepted: "+bad);
            } catch (IllegalArgumentException expected) {checks++;}
        }
        KillAuraScreenTodoAi.applyNumber(cfg.range,cfg.range.getClass().getField("rangeIncrease"),"2.5");
        var gson=new com.google.gson.Gson();
        cfg.enabled=true;
        var restored=gson.fromJson(gson.toJson(cfg),KillAuraConfigTodoAi.class);
        check(restored.enabled&&restored.range.rangeIncrease==2.5,"config JSON preserves enable and settings");
        System.out.println("PASS: "+checks+" KillAura geometry, smoothing and settings checks");
    }
}
