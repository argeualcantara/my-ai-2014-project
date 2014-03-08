package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Ladrao extends ProgramaLadrao {
	
//	
	private final Integer PESO_MINIMO = -20000;
	private final Integer PESO_MAXIMO = 20000;
	
	private final Integer PESO_PARADO = 5900;
	
	private final Integer PESO_PROXIMIDADE_MEDIA = 9400;
	private final Integer PESO_PROXIMIDADE_LONGA = 8390;
	
	private final Integer PESO_OLFATO_POUPADOR_UMA_UNIDADE = 1270;
	private final Integer PESO_OLFATO_POUPADOR_DUAS_UNIDADE = 1100;
	private final Integer PESO_OLFATO_POUPADOR_TRES_UNIDADE = 1000;
	private final Integer PESO_OLFATO_POUPADOR_QUATRO_UNIDADE = 900;
	private final Integer PESO_OLFATO_POUPADOR_CINCO_UNIDADE = 800;
	
	private final Integer PESO_JA_ANDOU_UM = 10;
	
	//Codigo percepcoes visiveis
	private final Integer VISAO_SEM_VISAO_LOCAL = -2;
	private final Integer VISAO_FORA_AMBIENTE = -1;
	private final Integer VISAO_PAREDE = 1;
	private final Integer VISAO_BANCO = 3;
	private final Integer VISAO_MOEDA = 4;
	private final Integer VISAO_PASTILHA_DO_PODER = 5;
	
	//Codigo percepcoes alfativas
	
	//Acoes
	private final Integer ACAO_CIMA = 1;
	private final Integer ACAO_BAIXO = 2;
	private final Integer ACAO_DIREITA = 3;
	private final Integer ACAO_ESQUERDA = 4;
	
	//Posicao
	
	private final Integer POSICAO_CIMA = 7;
	private final Integer POSICAO_BAIXO = 16;
	private final Integer POSICAO_ESQUERDA = 11;
	private final Integer POSICAO_DIRETIA = 12;
	
	//Pesos posicao
	private Integer peso_cima = 1;
	private Integer peso_baixo = 1;
	private Integer peso_direita = 1;
	private Integer peso_esquerda = 1;
	
	private Integer ultimaAcao = 0;
	private Integer qntTempoParado = 0;
	private Point lastPoint;
	private List<Integer> acoesValidas = new ArrayList<Integer>();
	private HashMap<String, MinhaPosicao> hashPosicoes = new HashMap<String, MinhaPosicao>();
	private List<MinhaPosicao> listPosicoes = new  ArrayList<Ladrao.MinhaPosicao>();
	boolean estavaBeco = false;
	private Integer ultimaParado = 0;
	boolean parado = false;
	int ultimaVisao;
	/* Medidas de desempenho
	 * 2 ladrões por poupador
	 * Se mais de 2 ladrões em um poupador o mais distante sai e vai atrás do outro poupador.
	 * Se quando tive mais de 2 ladrões em um mesmo poupador e a distância deles forem a mesma, nessa rodada todos os ladões vao atras do poupador. A menos que a distância de todos for 1, entao os ladroes pegam o poupador.
	 * */
	
	private List<Integer> naoPossoAndar = new ArrayList<Integer>();
	boolean debug = false;
	public int acao() {
		peso_cima = 1;
		peso_baixo = 1;
		peso_direita = 1;
		peso_esquerda = 1;
		acoesValidas = new ArrayList<Integer>();
		
		if(sensor.getPosicao().x == 1 && sensor.getPosicao().y == 0)
			debug = true;
//		if(debug)
//			System.out.println("entrar debug");
		
		
		int [] visao = sensor.getVisaoIdentificacao();
		
		analisarParado(visao);
		
		eliminarMovimentosNegativosObvios(visao);
		
		analisarProximidadeMaximaPoupador(visao);
		analisarProximidadeRetaPoupador(visao);
		analisarProximidadeMediaPoupador(visao);
		analisarProximidadeLongaPoupador(visao);

		analisarOlfatoPoupador();
		//analisarOlfatoLadrao();
		
		analisarParado();
		
		analisarLocalMenosVisitado();
		
		analisarMemoria();
		return agir();
	}
	
	private void analisarParado(int [] visao){
		Point myPoint = sensor.getPosicao();
		if(lastPoint != null && (lastPoint.x == myPoint.x && lastPoint.y == myPoint.y)){
			parado = true;
			qntTempoParado++;
		}else{
			parado = false;
			if(qntTempoParado > 0 && !estavaBeco){
				qntTempoParado--;
				ultimaParado = 0;
			}
		}
		estavaBeco = estouBeco(visao);
		lastPoint = myPoint;
	}
	
	private void analisarMemoria(){
		if (hashPosicoes.size() >= 50) {
			hashPosicoes.remove(listPosicoes.get(0).toString());
			listPosicoes.remove(0);
		}
	}
	
	private boolean estouBeco(int [] visao){
		int valorPosicao = visao[POSICAO_CIMA];
		int valorPosicao2 = visao[POSICAO_BAIXO];
		
		if (!possoAndar(valorPosicao) && !possoAndar(valorPosicao2))
			return true;
		valorPosicao = visao[POSICAO_DIRETIA];
		valorPosicao2 = visao[POSICAO_ESQUERDA];
		if (!possoAndar(valorPosicao) && !possoAndar(valorPosicao2))
				return true;
		return false;
	}
	
	private boolean possoAndar(int valor){
		
		if(valor == VISAO_PAREDE || valor == VISAO_FORA_AMBIENTE || valor == VISAO_BANCO || valor == VISAO_MOEDA || valor == VISAO_PASTILHA_DO_PODER)
			return false;
		return true;
	}
	
	private void analisarParado(){
		int peso = ((qntTempoParado * PESO_PARADO)*-1);
		if(ultimaParado == ACAO_BAIXO)
			atribuirPeso(POSICAO_BAIXO, peso);
		else if(ultimaParado == ACAO_CIMA)
			atribuirPeso(POSICAO_CIMA, peso);
		else if(ultimaParado == ACAO_ESQUERDA)
			atribuirPeso(POSICAO_ESQUERDA, peso);
		else if(ultimaParado == ACAO_DIREITA)
			atribuirPeso(POSICAO_DIRETIA, peso);
	}
	
	private void analisarOlfatoPoupador(){
		int [] olfato = sensor.getAmbienteOlfatoPoupador();
		
		for (int i = 0; i < olfato.length; i++) {
			if(i == 0) {
				atribuirPesoOlfatoPoupador(POSICAO_CIMA, olfato[i]);
				atribuirPesoOlfatoPoupador(POSICAO_ESQUERDA, olfato[i]);
			} else if (i == 1) {
				atribuirPesoOlfatoPoupador(POSICAO_CIMA, olfato[i]);
			}else if (i == 2) {
				atribuirPesoOlfatoPoupador(POSICAO_CIMA, olfato[i]);
				atribuirPesoOlfatoPoupador(POSICAO_DIRETIA, olfato[i]);
			}else if (i == 3) {
				atribuirPesoOlfatoPoupador(POSICAO_ESQUERDA, olfato[i]);
			}else if (i == 4) {
				atribuirPesoOlfatoPoupador(POSICAO_DIRETIA, olfato[i]);
			}else if (i == 5) {
				atribuirPesoOlfatoPoupador(POSICAO_BAIXO, olfato[i]);
				atribuirPesoOlfatoPoupador(POSICAO_ESQUERDA, olfato[i]);
			}else if (i == 6) {
				atribuirPesoOlfatoPoupador(POSICAO_BAIXO, olfato[i]);
			}else if (i == 7) {
				atribuirPesoOlfatoPoupador(POSICAO_BAIXO, olfato[i]);
				atribuirPesoOlfatoPoupador(POSICAO_DIRETIA, olfato[i]);
			}
				
		}
		
	}
	
	private void analisarOlfatoLadrao(){
		int [] olfato = sensor.getAmbienteOlfatoLadrao();
		for (int i = 0; i < olfato.length; i++) {
			if (i == 1) {
				if(ultimaAcao != ACAO_BAIXO)
					atribuirPesoOlfatoLadrao(POSICAO_CIMA, olfato[i]);
			}else if (i == 3 || i == 0 || i == 5) {
				if(ultimaAcao != ACAO_DIREITA)
					atribuirPesoOlfatoLadrao(POSICAO_ESQUERDA, olfato[i]);
			}else if (i == 4 || i == 2 || i == 7) {
				if(ultimaAcao != ACAO_ESQUERDA)
					atribuirPesoOlfatoLadrao(POSICAO_DIRETIA, olfato[i]);
			}else if (i == 6) {
				if(ultimaAcao != ACAO_CIMA)
					atribuirPesoOlfatoLadrao(POSICAO_BAIXO, olfato[i]);
			}
				
		}
		
	}
	
	private void atribuirPesoOlfatoPoupador(int posicao, int distancia) {
		int peso = 0;
		if(distancia == 1) {
			peso = PESO_OLFATO_POUPADOR_UMA_UNIDADE;
		}
		if(distancia == 2) {
			peso = PESO_OLFATO_POUPADOR_DUAS_UNIDADE;
		}
		if(distancia == 3) {
			peso = PESO_OLFATO_POUPADOR_TRES_UNIDADE;
		} 
		if(distancia == 4) {
			peso = PESO_OLFATO_POUPADOR_QUATRO_UNIDADE;
		} 
		if(distancia == 5) {
			peso = PESO_OLFATO_POUPADOR_CINCO_UNIDADE;
		} 
		
		if(posicao == POSICAO_BAIXO && peso_baixo < PESO_MAXIMO)
			peso_baixo = peso_baixo + peso;
		
		if(posicao == POSICAO_CIMA && peso_cima < PESO_MAXIMO)
			peso_cima = peso_cima + peso;
		
		if(posicao == POSICAO_ESQUERDA && peso_esquerda < PESO_MAXIMO)
			peso_esquerda = peso_esquerda + peso;
		
		if(posicao == POSICAO_DIRETIA && peso_direita < PESO_MAXIMO)
			peso_direita = peso_direita + peso;
	}
	
	private void atribuirPesoOlfatoLadrao(int posicao, int distancia) {
		int peso = 0;
		if(distancia == 1) {
			peso = PESO_OLFATO_POUPADOR_UMA_UNIDADE;
		}
		if(distancia == 2) {
			peso = PESO_OLFATO_POUPADOR_DUAS_UNIDADE;
		}
		if(distancia == 3) {
			peso = PESO_OLFATO_POUPADOR_TRES_UNIDADE;
		} 
		if(distancia == 4) {
			peso = PESO_OLFATO_POUPADOR_QUATRO_UNIDADE;
		} 
		if(distancia == 5) {
			peso = PESO_OLFATO_POUPADOR_CINCO_UNIDADE;
		}
		
		if(posicao == POSICAO_BAIXO && peso_baixo < PESO_MAXIMO)
			peso_baixo = peso_baixo + peso;
		
		if(posicao == POSICAO_CIMA && peso_cima < PESO_MAXIMO)
			peso_cima = peso_cima + peso;
		
		if(posicao == POSICAO_ESQUERDA && peso_esquerda < PESO_MAXIMO)
			peso_esquerda = peso_esquerda + peso;
		
		if(posicao == POSICAO_DIRETIA && peso_direita < PESO_MAXIMO)
			peso_direita = peso_direita + peso;
	}
	
	private void analisarProximidadeRetaPoupador(int [] visao) {
		int posicao = visao[2];
		if (posicao >= 100 && posicao <= 199) {
				atribuirPeso(POSICAO_CIMA, PESO_MAXIMO-20);
				int ladrao = visao[7];
				if(ladrao >= 200 && ladrao <=299){
					atribuirPeso(POSICAO_DIRETIA, PESO_MAXIMO-10);
					atribuirPeso(POSICAO_ESQUERDA, PESO_MAXIMO-10);
				}
		}
		
		posicao = visao[21];
		if (posicao >= 100 && posicao <= 199) {
				atribuirPeso(POSICAO_BAIXO, PESO_MAXIMO-20);
				int ladrao = visao[16];
				if(ladrao >= 200 && ladrao <=299){
					atribuirPeso(POSICAO_DIRETIA, PESO_MAXIMO-10);
					atribuirPeso(POSICAO_ESQUERDA, PESO_MAXIMO-10);
				}
		}
		
		posicao = visao[13];
		if (posicao >= 100 && posicao <= 199) {
				atribuirPeso(POSICAO_DIRETIA, PESO_MAXIMO-20);
				int ladrao = visao[12];
				if(ladrao >= 200 && ladrao <=299){
					atribuirPeso(POSICAO_CIMA, PESO_MAXIMO-10);
					atribuirPeso(POSICAO_BAIXO, PESO_MAXIMO-10);
				}
		}
		
		
		posicao = visao[10];
		if (posicao >= 100 && posicao <= 199) {
				atribuirPeso(POSICAO_ESQUERDA, PESO_MAXIMO-20);
				int ladrao = visao[11];
				if(ladrao >= 200 && ladrao <=299){
					atribuirPeso(POSICAO_CIMA, PESO_MAXIMO-10);
					atribuirPeso(POSICAO_BAIXO, PESO_MAXIMO-10);
				}
		}
		
	}
	
	private void analisarProximidadeMaximaPoupador(int [] visao){
		int valorPosicao = visao[POSICAO_CIMA];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
			atribuirPeso(POSICAO_CIMA, PESO_MAXIMO);
		}
		
		valorPosicao = visao[POSICAO_BAIXO];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
				atribuirPeso(POSICAO_BAIXO, PESO_MAXIMO);
		}
		
		valorPosicao = visao[POSICAO_DIRETIA];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
				atribuirPeso(POSICAO_DIRETIA, PESO_MAXIMO);
		}
		
		valorPosicao = visao[POSICAO_ESQUERDA];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
			
				atribuirPeso(POSICAO_ESQUERDA, PESO_MAXIMO);
			
		}
	}
	
	private void analisarProximidadeLongaPoupador(int [] visao){
		int valorPosicao1 = visao[0];
		int valorPosicao2 = visao[1];
		int valorPosicao3 =   visao[5];
		if ((valorPosicao1 >= 100 && valorPosicao1 <= 199) || (valorPosicao2 >= 100 && valorPosicao2 <= 199) || valorPosicao3 >= 100 && valorPosicao3 <= 199) {
			atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_LONGA);
			atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_LONGA);
			if(valorPosicao1 >= 100 && valorPosicao1 <= 199)
				atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_LONGA+5);
			if(valorPosicao3 >= 100 && valorPosicao3 <= 199)
				atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_LONGA+5);
		}
		
		valorPosicao1 = visao[3];
		valorPosicao2 = visao[4];
		valorPosicao3 = visao[9];
		if ((valorPosicao1 >= 100 && valorPosicao1 <= 199) || (valorPosicao2 >= 100 && valorPosicao2 <= 199) || (valorPosicao3 >= 100 && valorPosicao3 <= 199)) { 
			atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_LONGA);
			atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_LONGA);
			if(valorPosicao1 >= 100 && valorPosicao1 <= 199)
				atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_LONGA+5);
			if(valorPosicao3 >= 100 && valorPosicao3 <= 199)
				atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_LONGA+5);
		}
		
		valorPosicao1 = visao[22];
		valorPosicao2 = visao[23];
		valorPosicao3 = visao[18];
		if ((valorPosicao1 >= 100 && valorPosicao1 <= 199) || (valorPosicao2 >= 100 && valorPosicao2 <= 199) || (valorPosicao3 >= 100 && valorPosicao3 <= 199)) {
			atribuirPeso(POSICAO_BAIXO, PESO_PROXIMIDADE_LONGA);
			atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_LONGA);
			if(valorPosicao1 >= 100 && valorPosicao1 <= 199)
				atribuirPeso(POSICAO_BAIXO, PESO_PROXIMIDADE_LONGA+5);
			if(valorPosicao3 >= 100 && valorPosicao3 <= 199)
				atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_LONGA+5);
		}
		
		valorPosicao1 = visao[19];
		valorPosicao2 = visao[20];
		valorPosicao3 = visao[14];
		if ((valorPosicao1 >= 100 && valorPosicao1 <= 199) || (valorPosicao2 >= 100 && valorPosicao2 <= 199) || (valorPosicao2 >= 100 && valorPosicao2 <= 199)) {
			atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_LONGA);
			atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_LONGA);
			if(valorPosicao2 >= 100 && valorPosicao2 <= 199)
				atribuirPeso(POSICAO_BAIXO, PESO_PROXIMIDADE_LONGA+5);
			if(valorPosicao3 >= 100 && valorPosicao3 <= 199)
				atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_LONGA+5);
		}
		
	}
	
	private void analisarProximidadeMediaPoupador(int [] visao){
		int valorPosicao = visao[6];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
				atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_MEDIA);
				atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_MEDIA);
		}
		valorPosicao = visao[8];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
				atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_MEDIA);
				atribuirPeso(POSICAO_CIMA, PESO_PROXIMIDADE_MEDIA);
		}
		valorPosicao = visao[17];
		if (valorPosicao >= 100 && valorPosicao <= 199) {

				atribuirPeso(POSICAO_BAIXO, PESO_PROXIMIDADE_MEDIA);
				atribuirPeso(POSICAO_DIRETIA, PESO_PROXIMIDADE_MEDIA);
		}
		
		valorPosicao = visao[15];
		if (valorPosicao >= 100 && valorPosicao <= 199) {
				atribuirPeso(POSICAO_ESQUERDA, PESO_PROXIMIDADE_MEDIA);
				atribuirPeso(POSICAO_BAIXO, PESO_PROXIMIDADE_MEDIA);
		}
		
	}
	
	private void eliminarMovimentosNegativosObvios(int [] visao) {
		int posicao = visao[POSICAO_CIMA];
		
		if(posicao == VISAO_PAREDE || posicao == VISAO_MOEDA || posicao == VISAO_PASTILHA_DO_PODER || posicao == VISAO_FORA_AMBIENTE 
				|| posicao == VISAO_BANCO || posicao == VISAO_SEM_VISAO_LOCAL || (posicao >= 200 && posicao <= 299 ))
			atribuirPeso(POSICAO_CIMA, PESO_MINIMO);
		else
			acoesValidas.add(ACAO_CIMA);
		
		posicao = visao[POSICAO_BAIXO];
		if(posicao == VISAO_PAREDE || posicao == VISAO_MOEDA || posicao == VISAO_PASTILHA_DO_PODER || posicao == VISAO_FORA_AMBIENTE 
				|| posicao == VISAO_BANCO || posicao == VISAO_SEM_VISAO_LOCAL || (posicao >= 200 && posicao <= 299 ))
			atribuirPeso(POSICAO_BAIXO, PESO_MINIMO);
		else
			acoesValidas.add(ACAO_BAIXO);
		
		posicao = visao[POSICAO_DIRETIA];
		if(posicao == VISAO_PAREDE || posicao == VISAO_MOEDA || posicao == VISAO_PASTILHA_DO_PODER || posicao == VISAO_FORA_AMBIENTE 
				|| posicao == VISAO_BANCO || posicao == VISAO_SEM_VISAO_LOCAL || (posicao >= 200 && posicao <= 299 ))
			atribuirPeso(POSICAO_DIRETIA, PESO_MINIMO);
		else
			acoesValidas.add(ACAO_DIREITA);
		
		posicao = visao[POSICAO_ESQUERDA];
		if(posicao == VISAO_PAREDE || posicao == VISAO_MOEDA || posicao == VISAO_PASTILHA_DO_PODER || posicao == VISAO_FORA_AMBIENTE 
				|| posicao == VISAO_BANCO || posicao == VISAO_SEM_VISAO_LOCAL || (posicao >= 200 && posicao <= 299 ))
			atribuirPeso(POSICAO_ESQUERDA, PESO_MINIMO);
		else
			acoesValidas.add(ACAO_ESQUERDA);
		
	}
	
	private void atribuirPeso(int posicao, int valor) {
		if(valor > 0) {
			
			if(posicao == POSICAO_BAIXO && peso_baixo < PESO_MAXIMO)
				peso_baixo = peso_baixo + valor;
			
			if(posicao == POSICAO_CIMA && peso_cima < PESO_MAXIMO)
				peso_cima = peso_cima + valor;
			
			if(posicao == POSICAO_ESQUERDA && peso_esquerda < PESO_MAXIMO)
				peso_esquerda = peso_esquerda + valor;
			
			if(posicao == POSICAO_DIRETIA && peso_direita < PESO_MAXIMO)
				peso_direita = peso_direita + valor;
			
		} else if (valor < 0) {
			
			if(posicao == POSICAO_BAIXO && peso_baixo > PESO_MINIMO)
				peso_baixo = peso_baixo + valor;
			
			if(posicao == POSICAO_CIMA && peso_cima > PESO_MINIMO)
				peso_cima = peso_cima + valor;
			
			if(posicao == POSICAO_ESQUERDA && peso_esquerda > PESO_MINIMO)
				peso_esquerda = peso_esquerda + valor;
			
			if(posicao == POSICAO_DIRETIA && peso_direita > PESO_MINIMO)
				peso_direita = peso_direita + valor;
			
		}
	}
	
	private int agir(){
		
		if(peso_baixo == PESO_MINIMO && peso_cima == PESO_MINIMO && peso_direita == PESO_MINIMO && peso_esquerda == PESO_MINIMO)
			return 0;
		
		int maior_peso = -999999999;
		if (peso_baixo > maior_peso) {
			maior_peso = peso_baixo;
		}
			
		if (peso_cima > maior_peso) {
			maior_peso = peso_cima;
		}
		
		if (peso_direita > maior_peso) {
			maior_peso = peso_direita;
		}
		
		if (peso_esquerda > maior_peso) {
			maior_peso = peso_esquerda;
		} 

		List<Integer> results = new ArrayList<Integer>();
		//random iguais
		if (peso_baixo == maior_peso) {
			results.add(ACAO_BAIXO);
		}
		if (peso_cima == maior_peso) {
			results.add(ACAO_CIMA);
		}
		if (peso_direita == maior_peso) {
			results.add(ACAO_DIREITA);
		}
		if (peso_esquerda == maior_peso) {
			results.add(ACAO_ESQUERDA);
		}
		
		
		int resultado = (int)(Math.random() * results.size());
		int acaoDef = results.get(resultado);
		
		atualizarHashPosicoes(acaoDef);
		ultimaAcao = acaoDef;
		
		if(parado && ultimaParado == 0)
			ultimaParado = acaoDef;
		return acaoDef;
	}
	
	private void atualizarHashPosicoes(int acao) {
		int x = sensor.getPosicao().x;
		int y = sensor.getPosicao().y;
		if(acao == ACAO_BAIXO) {
			y = y + 1;
		} else if(acao == ACAO_CIMA) {
			y = y - 1;
		} else if(acao == ACAO_DIREITA) {
			x = x + 1;
		} else if(acao == ACAO_ESQUERDA) {
			x = x - 1;
		}
		
		String posicaoHash = x+","+y;
		MinhaPosicao posicao = hashPosicoes.get(posicaoHash);
		if (posicao == null) {
			MinhaPosicao minhaPosicao = new MinhaPosicao(x,y,1);
			hashPosicoes.put(minhaPosicao.toString(), minhaPosicao);
			listPosicoes.add(minhaPosicao);
		} else {
			posicao.incrementarPassagem();
			hashPosicoes.put(posicao.toString(), posicao);
		}
		
	}
	
	private void analisarLocalMenosVisitado(){
		int meuX = sensor.getPosicao().x;
		int meuY = sensor.getPosicao().y;
		
		MinhaPosicao minhaPosicaoEsq = hashPosicoes.get((meuX-1)+","+meuY);
		if (minhaPosicaoEsq == null) {
			minhaPosicaoEsq = new MinhaPosicao((meuX-1), meuY, 0);
			hashPosicoes.put(minhaPosicaoEsq.toString(), minhaPosicaoEsq);
			listPosicoes.add(minhaPosicaoEsq);
		}
		meuX = sensor.getPosicao().x;
		meuY = sensor.getPosicao().y;
		MinhaPosicao minhaPosicaoDir = hashPosicoes.get((meuX+1)+","+meuY);
		if (minhaPosicaoDir == null) {
			minhaPosicaoDir = new MinhaPosicao((meuX+1), meuY, 0);
			hashPosicoes.put(minhaPosicaoDir.toString(), minhaPosicaoDir);
			listPosicoes.add(minhaPosicaoDir);
		}
		meuX = sensor.getPosicao().x;
		meuY = sensor.getPosicao().y;
		MinhaPosicao minhaPosicaoCima = hashPosicoes.get(meuX+","+(meuY-1));
		if (minhaPosicaoCima == null) {
			minhaPosicaoCima = new MinhaPosicao(meuX, (meuY-1), 0);
			hashPosicoes.put(minhaPosicaoCima.toString(), minhaPosicaoCima);
			listPosicoes.add(minhaPosicaoCima);
		}
		meuX = sensor.getPosicao().x;
		meuY = sensor.getPosicao().y;
		MinhaPosicao minhaPosicaoBaixo = hashPosicoes.get(meuX+","+(meuY+1));
		if (minhaPosicaoBaixo == null) {
			minhaPosicaoBaixo = new MinhaPosicao(meuX, (meuY+1), 0);
			hashPosicoes.put(minhaPosicaoBaixo.toString(), minhaPosicaoBaixo);
			listPosicoes.add(minhaPosicaoBaixo);
		}
		
		HashMap<String, Integer> hashOrganizar = new HashMap<String, Integer>();
		hashOrganizar.put(minhaPosicaoBaixo.toString(), ACAO_BAIXO);
		hashOrganizar.put(minhaPosicaoCima.toString(), ACAO_CIMA);
		hashOrganizar.put(minhaPosicaoDir.toString(), ACAO_DIREITA);
		hashOrganizar.put(minhaPosicaoEsq.toString(), ACAO_ESQUERDA);
		
		List<MinhaPosicao> naoOrdenado = new ArrayList<MinhaPosicao>();
		for (int i = 0; i < acoesValidas.size(); i++) {
			if(hashOrganizar.get(minhaPosicaoBaixo.toString()) == acoesValidas.get(i))
				naoOrdenado.add(minhaPosicaoBaixo);
		}
		for (int i = 0; i < acoesValidas.size(); i++) {
			if(hashOrganizar.get(minhaPosicaoCima.toString()) == acoesValidas.get(i))
				naoOrdenado.add(minhaPosicaoCima);
		}
		for (int i = 0; i < acoesValidas.size(); i++) {
			if(hashOrganizar.get(minhaPosicaoDir.toString()) == acoesValidas.get(i))
				naoOrdenado.add(minhaPosicaoDir);
		}
		for (int i = 0; i < acoesValidas.size(); i++) {
			if(hashOrganizar.get(minhaPosicaoEsq.toString()) == acoesValidas.get(i))
				naoOrdenado.add(minhaPosicaoEsq);
		}
		
		MinhaPosicao menor = pegarMenor(naoOrdenado);
		if(menor != null) {
			if(hashOrganizar.get(menor.toString()) == ACAO_BAIXO)
				atribuirPeso(POSICAO_BAIXO, PESO_JA_ANDOU_UM);
			if(hashOrganizar.get(menor.toString()) == ACAO_CIMA)
				atribuirPeso(POSICAO_CIMA, PESO_JA_ANDOU_UM);
			if(hashOrganizar.get(menor.toString()) == ACAO_DIREITA)
				atribuirPeso(POSICAO_DIRETIA, PESO_JA_ANDOU_UM);
			if(hashOrganizar.get(menor.toString()) == ACAO_ESQUERDA)
				atribuirPeso(POSICAO_ESQUERDA, PESO_JA_ANDOU_UM);
		}
		
		for (int i = 0; i < naoOrdenado.size(); i++) {
			if(menor.getQntPassei() == naoOrdenado.get(i).getQntPassei() && !menor.toString().equals(naoOrdenado.get(i).toString())){
				if(hashOrganizar.get(naoOrdenado.get(i).toString()) == ACAO_BAIXO)
					atribuirPeso(POSICAO_BAIXO, PESO_JA_ANDOU_UM);
				if(hashOrganizar.get(naoOrdenado.get(i).toString()) == ACAO_CIMA)
					atribuirPeso(POSICAO_CIMA, PESO_JA_ANDOU_UM);
				if(hashOrganizar.get(naoOrdenado.get(i).toString()) == ACAO_DIREITA)
					atribuirPeso(POSICAO_DIRETIA, PESO_JA_ANDOU_UM);
				if(hashOrganizar.get(naoOrdenado.get(i).toString()) == ACAO_ESQUERDA)
					atribuirPeso(POSICAO_ESQUERDA, PESO_JA_ANDOU_UM);
			}
		}
		
	}
	private MinhaPosicao pegarMenor(List<MinhaPosicao> naoOrdenado){
		if(naoOrdenado.size() > 0) {
			MinhaPosicao menor = naoOrdenado.get(0);
			for (MinhaPosicao minhaPosicao : naoOrdenado) {
				if(minhaPosicao.getQntPassei() < menor.getQntPassei())
					menor = minhaPosicao;
			}
			return menor;
		}
		
		return null;
	}
	
	class MinhaPosicao{
		
		private int x;
		private int y;
		private int qntPassei;
		
		public MinhaPosicao(int x, int y, int qntPassei){
			this.x = x;
			this.y = y;
			this.qntPassei = qntPassei;
		}
		
		public int getQntPassei() {
			return qntPassei;
		}
		public void incrementarPassagem() {
			qntPassei++;
		}
		@Override
		public String toString() {
			return x+","+y;
		}
	}


}