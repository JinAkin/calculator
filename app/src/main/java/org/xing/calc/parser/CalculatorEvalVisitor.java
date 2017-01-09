package org.xing.calc.parser;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.xing.calc.parser.grammer.calculatorBaseVisitor;
import org.xing.calc.parser.grammer.calculatorParser;
import org.xing.calc.parser.grammer.calculatorParser.FuncContext;
import org.xing.calc.parser.grammer.calculatorParser.FuncnameContext;
import org.xing.calc.parser.grammer.calculatorParser.FuncnameExContext;
import org.xing.calc.parser.grammer.calculatorParser.PostFuncnameContext;

public class CalculatorEvalVisitor extends calculatorBaseVisitor<Double> {
	private NumberParser numParser;
	public CalculatorEvalVisitor() {
		numParser = new NumberParser();
	}
	
	@Override
	public Double visitExpression(calculatorParser.ExpressionContext ctx) {
		Double result = visit(ctx.getChild(0));
		if(ctx.getChildCount() > 2) {
			for(int i=1;i<ctx.getChildCount();i+=2) {
				TerminalNode oper = (TerminalNode)ctx.getChild(i);
				Double exprValue = visit(ctx.getChild(i+1));
				if(oper.getSymbol().getType() == calculatorParser.PLUS) {
					result = result + exprValue;
				} else {
					result = result - exprValue;
				}
			}
		}
		return result;
	}

	@Override
	public Double visitMultiplyingExpression(
			calculatorParser.MultiplyingExpressionContext ctx) {
		Double result = visit(ctx.getChild(0));
		
		if(ctx.getChildCount() > 2) {
			for(int i=1;i<ctx.getChildCount();i+=2) {
				
				TerminalNode oper = (TerminalNode)ctx.getChild(i);
				Double exprValue = visit(ctx.getChild(i+1));
				if(oper.getSymbol().getType() == calculatorParser.DIV) {
					result = result / exprValue;
				} else {
					result = result * exprValue;
				}
			}
		}
		return result;
	}

	@Override
	public Double visitPowExpression(calculatorParser.PowExpressionContext ctx) {
		Double result = visit(ctx.getChild(0));
		
		if(ctx.getChildCount() > 1) {	
			for(int i=2;i<ctx.getChildCount();i+=2) {
				Double pow = visit(ctx.getChild(i));
				result = Math.pow(result, pow);
			}
		}
		
		return result;
	}
	
	@Override 
	public Double visitChinaPowExpression(
			calculatorParser.ChinaPowExpressionContext ctx) { 
		Double result = Double.NaN;

		int index = 0;
		result = visit(ctx.getChild(index++));
		while (index < ctx.getChildCount()) {
			if (ctx.getChild(index + 1) instanceof TerminalNode) {
				int type = ((TerminalNode) ctx.getChild(index + 1)).getSymbol()
						.getType();
				if (type == calculatorParser.PINGFANG) {
					result = Math.pow(result, 2);
				} else if (type == calculatorParser.LIFANG) {
					result = Math.pow(result, 3);
				} else {
					System.err.println("未处理的终端节点:visitChinaPowExpression");
				}
				index += 2;
			} else {
				Double pow = visit(ctx.getChild(index + 1));
				result = Math.pow(result, pow);
				index += 3;
			}
		}
		return result;
	}

	@Override
	public Double visitAtom(calculatorParser.AtomContext ctx) {
		if(ctx.getChildCount() == 3) {
			if(ctx.FRAC() != null) {
				if(ctx.FRAC().getText().equals("分之")) {
					return visit(ctx.getChild(2)) / visit(ctx.getChild(0));
				}else {
					//分数用'/'表示，这是语音识别的结果导致的，它偶尔自动把分数转化成了'/'
					return visit(ctx.getChild(0)) / visit(ctx.getChild(2));
				}
			}else {
				return visit(ctx.expression());
			}
		}else {
			return visit(ctx.getChild(0));
		}
	}

	@Override
	public Double visitFunc(FuncContext ctx) {
		Double result = Double.NaN;
		if(ctx.funcnameEx() != null) {
			FuncnameExContext func = ctx.funcnameEx();
			Double firstNum = visit(ctx.getChild(0));
			Double secondNum = visit(ctx.getChild(2));
			if(func.DUISHU() != null) {
				result = Math.log(secondNum) / Math.log(firstNum);
			}else if(func.GENHAO() != null) {
				result = Math.pow(secondNum, 1/firstNum);
			}
		} else if(ctx.funcname() != null){
			FuncnameContext func = ctx.funcname();
			Double exprValue = visit(ctx.getChild(1));

			if(exprValue.isNaN()) return result;
			
			if(func.SIN() != null) {
				result = Math.sin(exprValue);
			}else if(func.COS() != null) {
				result = Math.cos(exprValue);
			}else if(func.TAN() != null) {
				result = Math.tan(exprValue);
			}else if(func.ASIN() != null) {
				result = Math.asin(exprValue);
			}else if(func.ACOS() != null) {
				result = Math.acos(exprValue);
			}else if(func.ATAN() != null) {
				result = Math.atan(exprValue);
			}else if(func.LG() != null) {
				result = Math.log10(exprValue);
			}else if(func.LOG() != null) {
				result =  Math.log(exprValue) / Math.log(2);
			}else if(func.LN() != null) {
				result =  Math.log(exprValue);
			}else if(func.GENHAO() != null) {
				result =  Math.pow(exprValue, 0.5);
			}else if(func.DUISHU() != null) {
				result =  Math.log(exprValue);
			}else{
				System.err.println("Not supported function '"+ctx.funcname()+"'");
			}	
		}else if(ctx.postFuncname() != null) {
			PostFuncnameContext postFuncname = ctx.postFuncname();
			Double exprValue = visit(ctx.getChild(0));
			
			if(postFuncname.KAIFANG() != null 
					|| postFuncname.KAIPINGFANG() != null
					|| postFuncname.PINGFANG() != null) {
				result =  Math.pow(exprValue, 0.5);
			}else if(postFuncname.KAILIFANG() != null
					|| postFuncname.LIFANG() != null) {
				result =  Math.pow(exprValue, 1.0/3);
			}
		}
		
		return result;
	}

	@Override
	public Double visitNumber(calculatorParser.NumberContext ctx) {
		try{
			String expr = ctx.getText();
			if(ctx.PAI() != null || ctx.DU() != null) {
				if(ctx.DIGIT().isEmpty()) {
					if(ctx.MINUS() != null) {
						return -Math.PI;
					}else {
						return Math.PI;
					}
				}

				numParser.parse(expr.substring(0, expr.length()-1));
				Double result = Double.parseDouble(numParser.getEvalExpr());
				if(ctx.PAI() != null) {
					return result * Math.PI;
				}else {
					return result * Math.PI / 180;
				}
			} else {
				numParser.parse(expr);
				return Double.parseDouble(numParser.getEvalExpr());
			}

		}catch(Exception ex) {
			return Double.NaN;
		}
	}
}
