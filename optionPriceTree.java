import java.lang.reflect.Method;
import java.lang.Math.*;
import java.lang.*;
import java.util.*;
//extremely general "Tree" class for computing options for single dimensional SDEs.  
//given an SDE dX=alpha(X)dt+sigma(X)dW_t, this computes the (recombining) tree.  
//Inputs are "functions" given as a java method
//functions are alpha(x)/sigma(x)
//derivative of sigma with respect to x
//inverse of f where f=\int 1/sigma(x)
//payoff function g(X)
//discount factor d(X)
//non function inputs are time to maturity "maturity", number of time steps "m", (constant) interest rate "r", initial value of f(x) "x0"
//for interest rate options, r should not be constant.
public class optionPriceTree{ //takes "link" function to generate trees of 
    private Method alpha;//alpha(x)/sigma(x)
    private Method sigma; //sigma_x
    private Method fInv; //f^{-1}
    private Method payoff; //payoff function
    private Method discountFactor; //payoff function
    private int m;
    private double maturity;
    private double dt;
    private long timeToRun=0;
    private double finalSolution=0;
  //  private double r;
    private int totalTimesProbabilityChanged=0;
    private double x0;
    private Object callingClass;
    public optionPriceTree(){
        this.m=100;
        this.maturity=1;
        this.dt=maturity/(double)m;
        //this.r=.05;
        this.x0=0; //in default case, this is stock=1 (exp(0)=1)
        this.callingClass=this; //by default, current class is the "correct" class
        try {
            this.alpha=this.getClass().getDeclaredMethod("sampleAlpha", new Class[]{Double.class});
            this.sigma=this.getClass().getDeclaredMethod("sampleSigma", new Class[]{Double.class}); //default methods
            this.fInv=this.getClass().getDeclaredMethod("sampleFinv", new Class[]{Double.class}); //default methods
            this.payoff=this.getClass().getDeclaredMethod("samplePayoff", new Class[]{Double.class}); //default methods
            this.discountFactor=this.getClass().getDeclaredMethod("samplediscountFactor", new Class[]{Double.class, Double.class}); //default methods
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
    public optionPriceTree(Method alpha, Method sigma, Method fInv,  Method discountFactor, Method payoff, int m, double maturity, double x0, Object callingClass){
        this.alpha=alpha;
        this.sigma=sigma;
        this.fInv=fInv;
        this.m=m;
        this.payoff=payoff;
        this.maturity=maturity;
        this.dt=maturity/(double)m;
       // this.r=r;
        this.x0=x0;
        this.discountFactor=discountFactor;
        this.callingClass=callingClass;
    }
    private double computeP(double x){
        double p=0;
        try{
            //double u=(actualx+Math.sqrt(dt))*(actualx+Math.sqrt(dt))*.25*.5*.5;
           // double d=(actualx-Math.sqrt(dt))*(actualx-Math.sqrt(dt))*.25*.5*.5;
            //p=((Double)alpha.invoke(callingClass, x)*dt+x-d)/(u-d);
            p=((Double)alpha.invoke(callingClass, x)-(Double)sigma.invoke(callingClass, x)*.5)*.5*Math.sqrt(dt)+.5;
            if(p<0){
                p=0;
                totalTimesProbabilityChanged++;
            }
            if(p>1){
                p=1;
                totalTimesProbabilityChanged++;
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
        return(p);
    }
    
    private double sampleAlpha(Double x){ //alpha(x)/sigma(x)
        return .05/.3;
    } 
    private double sampleSigma(Double x){ //derivative of sigma(x)
        return .3;
    }
    private double sampleFinv(Double x){ //GBM (sigma=.3)
        return Math.exp(.3*x);
    }
    private double samplePayoff(Double x){ //put option (strike 1)
        double py=1.0-x;
        if(py<0){
            py=0;
        }
        return py;
    }
    private double samplediscountFactor(Double x, Double dt){ //put option (strike 1)
        return Math.exp(-.05*dt);
    }
    public void computeTree(boolean isAmerican){
        totalTimesProbabilityChanged=0;
        long startTime = System.currentTimeMillis();
        List<Double> treeDerivative=new ArrayList<Double>();
        double underlying=0;
        try {
            for(int i=0; i<m+1; i++){
                underlying=(Double)fInv.invoke(callingClass, x0+Math.sqrt(dt)*(m-2*i));
                treeDerivative.add((Double)payoff.invoke(callingClass, underlying)); //derivative tree at termination
            }
           
            createTree(isAmerican, treeDerivative);
        }
        catch(Exception e){
            System.out.println(e);
        
        }
        long endTime = System.currentTimeMillis();
        timeToRun=endTime-startTime;
    }

    private void createTree(boolean isAmerican, List<Double> treeDerivative) throws Exception {
        int remainingStep=treeDerivative.size()-1;
        double optionValue=0;
        double intrinsicValue=0;
        if(remainingStep==0){
            optionValue=treeDerivative.get(0);
            // if(isAmerican){
                // intrinsicValue=(Double)payoff.invoke(this, x0);
                // if(intrinsicValue>optionValue){
                    // optionValue=intrinsicValue;
                // }
            // }
            finalSolution=optionValue;
        }
        else {
            try {
                double underlying=0;
                double p=0;
                for(int i=0; i<remainingStep; i++){
                    underlying=(Double)fInv.invoke(callingClass, x0+Math.sqrt(dt)*(remainingStep-1-2*i));
                    p=computeP(underlying);
                    intrinsicValue=(Double)payoff.invoke(callingClass, underlying);
                    optionValue=(Double)discountFactor.invoke(callingClass,  new Object[]{underlying, dt})*(p*treeDerivative.get(i)+(1.0-p)*treeDerivative.get(i+1));
                    if(isAmerican){
                        if(intrinsicValue>optionValue){
                            optionValue=intrinsicValue;
                        }
                    }
                    treeDerivative.set(i, optionValue); //shouldn't overwrite anything useful
                    
                }
                treeDerivative.remove(remainingStep); //remove last value in list
                createTree(isAmerican, treeDerivative);
            }
            catch(Exception e){
                throw(e);
            }
        }
    }
    public long getRunTime() {
        return timeToRun;
    }
    public double getOptionValue() {
        return finalSolution;
    }
    public int getTimesProbabilityChanged(){
        return totalTimesProbabilityChanged;
    }
    
    
}