import java.util.*;
import java.lang.reflect.Method;
import java.lang.Math.*;
public class optionTesting {
    private double r0=.03;
    private double a=1;
    private double sigma=.3; 
    private double b=.05; //long run r
    private int m=1000;
    private double maturity=1.0;
    public optionTesting() {
        try{
            double x0=2.0*Math.sqrt(r0)/sigma;
            Method alph=this.getClass().getDeclaredMethod("alpha", new Class[]{Double.class});
            Method sigs=this.getClass().getDeclaredMethod("sigmax", new Class[]{Double.class});
            Method fin=this.getClass().getDeclaredMethod("finv", new Class[]{Double.class});
            Method disc=this.getClass().getDeclaredMethod("discount", new Class[]{Double.class, Double.class});
            Method pyoff=this.getClass().getDeclaredMethod("payoff", new Class[]{Double.class});
            if(a*b*2>sigma*sigma){
                optionPriceTree testOption=new optionPriceTree(alph, sigs, fin, disc, pyoff, m, maturity, x0, this);
                testOption.computeTree(false);
                System.out.print(testOption.getOptionValue());
                System.out.println(", runtime: "+testOption.getRunTime());
                System.out.println(", timesChanges: "+testOption.getTimesProbabilityChanged());
            }
            else {
                System.out.println("Feller condition not satisfied!");
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public static void main(String[] args) {
        optionTesting ops=new optionTesting();
    }
    public double alpha(Double x){
        return((a*(b-x))/(sigma*Math.sqrt(x)));
    }
    public double sigmax(Double x){
        return(.5*sigma/(Math.sqrt(x)));
    }
    public double finv(Double x){
        return(sigma*sigma*x*x*.25);
    }
    public double discount(Double x, Double dt){
        //return(1.0/(1+dt*x));
        return(Math.exp(-dt*x));
    }
    public double payoff(Double x){
        return(1.0);
    }

}