package algoritmo;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import controle.Constantes;

public class Poupador extends ProgramaPoupador {

	// Constantes da matriz de vis�o
	private static final int SEM_VISAO = -2;
	private static final int FORA_AMBIENTE = -1;
	private static final int PAREDE = 1;
	private static final int BANCO = 3;
	private static final int MOEDA = 4;
	private static final int PASTILHA_PODER = 5;
	private int[] pesos;
	private ArrayList<Integer> esquerda;
	private ArrayList<Integer> cima;
	private ArrayList<Integer> direita;
	private ArrayList<Integer> baixo;
	private ArrayList<Integer> perto;
	private int[] visao;
	private HashMap<Point, Integer> pontosVisitados;

	public Poupador() {
		this.pontosVisitados = new HashMap<Point, Integer>();
		esquerda = new ArrayList<Integer>(Arrays.asList(10, 11, 0, 1, 5, 6, 14, 15, 19, 20));
		cima = new ArrayList<Integer>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
		direita = new ArrayList<Integer>(Arrays.asList(12, 13, 3, 4, 8, 9, 17, 18, 22, 23));
		baixo = new ArrayList<Integer>(Arrays.asList(14, 15, 16, 17, 18, 19, 20, 21, 22, 23));
		perto = new ArrayList<Integer>(Arrays.asList(6, 7, 8, 11, 12, 15, 16, 17));
	}

	public int acao() {
		this.pesos = new int[24];		
		pesoPontoAtual(sensor.getPosicao());
		analisarLocaisVisitados();
		analisarVisao();
		analisarOlfato(sensor.getAmbienteOlfatoLadrao(),true);
		analisarOlfato(sensor.getAmbienteOlfatoPoupador(), false);
		return decidirMovimento();
	}

	// reduz o peso do ponto atual
	private void pesoPontoAtual(Point p) {
		if (pontosVisitados.containsKey(p)) {
			int peso = pontosVisitados.get(p);
			peso -= 50;
			pontosVisitados.put(p, peso);
		} else {
			pontosVisitados.put(p, -50);
		}
	}

	// torna mais dif�cil a visita��o de pontos j� visitados
	private void analisarLocaisVisitados() {
		Point p = sensor.getPosicao();
		Point cima = new Point(p.x, p.y - 1);
		Point baixo = new Point(p.x, p.y + 1);
		Point esquerda = new Point(p.x - 1, p.y);
		Point direita = new Point(p.x + 1, p.y);
		pesos[7] += (pontosVisitados.get(cima) == null ? 0 : pontosVisitados.get(cima));
		pesos[12] += (pontosVisitados.get(direita) == null ? 0 : pontosVisitados.get(direita));
		pesos[16] += (pontosVisitados.get(baixo) == null ? 0 : pontosVisitados.get(baixo));
		pesos[11] += (pontosVisitados.get(esquerda) == null ? 0 : pontosVisitados.get(esquerda));

	}

	// seta os pesos de acordo com a vis�o do agente

	public void analisarVisao() {
		// as posi��es do campo de vis�o, numeradas de 0 a 23 assim como no PDF
		// do trabalho
		visao = sensor.getVisaoIdentificacao();
		for (int i = 0; i < visao.length; i++) {
			switch (visao[i]) {
			case SEM_VISAO:
				this.pesos[i] += -200;
				break;
			case FORA_AMBIENTE:
				this.pesos[i] += -600;
				break;
			case PAREDE:
				this.pesos[i] += -600;
				break;
			case BANCO:
				this.pesos[i] += 500 * (sensor.getNumeroDeMoedas());
				break;
			case MOEDA:
				this.pesos[i] += 1400;
				break;
			case PASTILHA_PODER:
				this.pesos[i] += (sensor.getNumeroDeMoedas() < 5)? -500 : 400;
			default:

				if (visao[i] >= 100) {
					// � outro poupador ou um ladr�o
					this.pesos[i] += -1000;
				} else {
					this.pesos[i] += -5;
				}
				break;
			}

		}
	}

	public void analisarOlfato(int[] olfato, boolean ladrao) {
		if(ladrao){
			for (int i = 0; i < olfato.length; i++) {
				
				this.pesos[perto.get(i)] += (olfato[i] == 0) ? 1000 : -1000 * (5 - olfato[i]);
				if(sensor.getNumeroDeMoedas() == 0){
					this.pesos[perto.get(i)] += 1500 * (5 - olfato[i]);
				}
			}
		}
		else{
			for (int i = 0; i < olfato.length; i++) {
				
				this.pesos[perto.get(i)] += (olfato[i] == 0) ? 1000 : -500 * (5 - olfato[i]);
			}
		}

	}

	public int decidirMovimento() {

		// considerar a posi��o do banco
		Point banco = Constantes.posicaoBanco;
		if (banco.x < sensor.getPosicao().x) {
			pesos[11] += sensor.getNumeroDeMoedas() * 50;
		}

		if (banco.x > sensor.getPosicao().x) {

			pesos[12] += sensor.getNumeroDeMoedas() * 50;
		}

		if (banco.y < sensor.getPosicao().y) {

			pesos[7] += sensor.getNumeroDeMoedas() * 50;
		}

		if (banco.y > sensor.getPosicao().y) {

			pesos[16] += sensor.getNumeroDeMoedas() * 50;
		}

		// resumir os pesos para apenas as 4 dire��es poss�veis de movimento

		int pesoEsquerda = somarPesos(esquerda) + pesoObstaculo(11);
		int pesoDireita = somarPesos(direita) + pesoObstaculo(12);
		int pesoCima = somarPesos(cima) + pesoObstaculo(7);
		int pesoBaixo = somarPesos(baixo) + pesoObstaculo(16);

		int[] pesosDirecao = { pesoCima, pesoBaixo, pesoDireita, pesoEsquerda };

		int maiorPeso = -999999;
		int direcao = -1;

		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] > maiorPeso) {
				maiorPeso = pesosDirecao[i];
				direcao = i + 1;
			}
		}

		ArrayList<Integer> pesosIguais = new ArrayList<Integer>();
		// verifica se tem mais de um maiorPeso
		for (int i = 0; i < pesosDirecao.length; i++) {
			if (pesosDirecao[i] == maiorPeso) {
				pesosIguais.add(i + 1);
			}
		}

		if (pesosIguais.size() > 1) {
			int indice = (int) (Math.random() * (pesosIguais.size()));
			direcao = pesosIguais.get(indice);

		}

		/*
		 * os valores de retorno s�o 0: ficar parado 1: ir pra cima 2: ir pra
		 * baixo 3: ir pra direita 4: ir pra esquerda
		 */

		System.out.println(direcao + " " + pesoCima + " " + pesoBaixo + " " + pesoDireita + " " + pesoEsquerda + " " + sensor.getPosicao());

		if ((direcao < 1) || (direcao > 4)) {
			return 0;
		}
		return direcao;

	}

	private int pesoObstaculo(int posicao) {

		if ((visao[posicao] == PAREDE) || (visao[posicao] == FORA_AMBIENTE) || (visao[posicao] >= 100)) {
			return -5000;
		}

		if ((visao[posicao] == PASTILHA_PODER) && (sensor.getNumeroDeMoedas() < 5)) {
			return -5000;
		}

		if ((visao[posicao] == BANCO) && (sensor.getNumeroDeMoedas() == 0)) {
			return -4000;
		}

		return 0;

	}

	private int somarPesos(ArrayList<Integer> direcao) {
		int soma = 0;
		for (int i : direcao) {

			soma += pesos[i];

		}
		return soma;
	}

}