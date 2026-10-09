import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

enum Cor {
    BRANCA, PRETA;

    public Cor oposta() {
        return this == BRANCA ? PRETA : BRANCA;
    }
}

class Posicao {
    private final int linha;
    private final int coluna;

    public Posicao(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    public int getLinha()  { return linha; }
    public int getColuna() { return coluna; }

    public boolean isValida() {
        return linha >= 0 && linha < 8 && coluna >= 0 && coluna < 8;
    }

    public Posicao deslocar(int deltaLinha, int deltaColuna) {
        return new Posicao(linha + deltaLinha, coluna + deltaColuna);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Posicao)) return false;
        Posicao outra = (Posicao) obj;
        return linha == outra.linha && coluna == outra.coluna;
    }

    @Override
    public int hashCode() { return 31 * linha + coluna; }

    @Override
    public String toString() { 
        return "" + (char) ('a' + coluna) + (8 - linha);
    }
}

class Movimento {

    public enum Tipo { NORMAL, ROQUE_MENOR, ROQUE_MAIOR, EN_PASSANT, PROMOCAO }

    private final Posicao origem;
    private final Posicao destino;
    private final Tipo tipo;
    private final Peca pecaPromocao; 
    private Peca pecaMovida;
    private boolean pecaJaMoveuAntes;
    private Peca pecaCapturada;
    private Posicao posicaoCapturada;
    private Peca torreMovida;           
    private boolean torreJaMoveuAntes;  
    private Posicao alvoEnPassantAnterior;

    public Movimento(Posicao origem, Posicao destino, Tipo tipo) {
        this(origem, destino, tipo, null);
    }

    public Movimento(Posicao origem, Posicao destino, Tipo tipo, Peca pecaPromocao) {
        this.origem = origem;
        this.destino = destino;
        this.tipo = tipo;
        this.pecaPromocao = pecaPromocao;
    }

    public Posicao getOrigem()  { return origem; }
    public Posicao getDestino() { return destino; }
    public Tipo getTipo()       { return tipo; }
    public Peca getPecaPromocao() { return pecaPromocao; }

    public Peca getPecaMovida()          { return pecaMovida; }
    public boolean isPecaJaMoveuAntes()  { return pecaJaMoveuAntes; }
    public Peca getPecaCapturada()       { return pecaCapturada; }
    public Posicao getPosicaoCapturada() { return posicaoCapturada; }
    public Peca getTorreMovida()         { return torreMovida; }
    public boolean isTorreJaMoveuAntes() { return torreJaMoveuAntes; }
    public Posicao getAlvoEnPassantAnterior() { return alvoEnPassantAnterior; }

    void setPecaMovida(Peca p)          { this.pecaMovida = p; }
    void setPecaJaMoveuAntes(boolean v) { this.pecaJaMoveuAntes = v; }
    void setPecaCapturada(Peca p)       { this.pecaCapturada = p; }
    void setPosicaoCapturada(Posicao p) { this.posicaoCapturada = p; }
    void setTorreMovida(Peca p)         { this.torreMovida = p; }
    void setTorreJaMoveuAntes(boolean v){ this.torreJaMoveuAntes = v; }
    void setAlvoEnPassantAnterior(Posicao p) { this.alvoEnPassantAnterior = p; }
}

abstract class Peca {

    private final Cor cor;
    private boolean jaMoveu;

    protected Peca(Cor cor) {
        this.cor = cor;
    }

    public Cor getCor() { return cor; }
    public boolean jaMoveu() { return jaMoveu; }
    public void setJaMoveu(boolean jaMoveu) { this.jaMoveu = jaMoveu; }

    public boolean ehInimiga(Peca outra) {
        return outra != null && outra.getCor() != cor;
    }

    public abstract List<Movimento> movimentosPseudoLegais(Tabuleiro tabuleiro, Posicao origem);

    public abstract String getSimbolo();

    @Override
    public String toString() {
        return getClass().getSimpleName() + " " + (cor == Cor.BRANCA ? "branca" : "preta");
    }
}

abstract class PecaDeslizante extends Peca {

    protected PecaDeslizante(Cor cor) { super(cor); }

    protected abstract int[][] direcoes();

    @Override
    public List<Movimento> movimentosPseudoLegais(Tabuleiro tabuleiro, Posicao origem) {
        List<Movimento> movimentos = new ArrayList<>();
        for (int[] direcao : direcoes()) {
            Posicao alvo = origem.deslocar(direcao[0], direcao[1]);
            while (alvo.isValida()) {
                Peca noCaminho = tabuleiro.pecaEm(alvo);
                if (noCaminho == null) {
                    movimentos.add(new Movimento(origem, alvo, Movimento.Tipo.NORMAL));
                } else {
                    if (ehInimiga(noCaminho)) {
                        movimentos.add(new Movimento(origem, alvo, Movimento.Tipo.NORMAL));
                    }
                    break; 
                }
                alvo = alvo.deslocar(direcao[0], direcao[1]);
            }
        }
        return movimentos;
    }
}

class Peao extends Peca {

    public Peao(Cor cor) { super(cor); }

    private int direcao() { return getCor() == Cor.BRANCA ? -1 : 1; }

    private int linhaInicial() { return getCor() == Cor.BRANCA ? 6 : 1; }

    @Override
    public List<Movimento> movimentosPseudoLegais(Tabuleiro tabuleiro, Posicao origem) {
        List<Movimento> movimentos = new ArrayList<>();
        int direcao = direcao();

        Posicao umaCasa = origem.deslocar(direcao, 0);
        if (umaCasa.isValida() && tabuleiro.pecaEm(umaCasa) == null) {
            adicionar(movimentos, origem, umaCasa);

            if (origem.getLinha() == linhaInicial()) {
                Posicao duasCasas = origem.deslocar(2 * direcao, 0);
                if (tabuleiro.pecaEm(duasCasas) == null) {
                    movimentos.add(new Movimento(origem, duasCasas, Movimento.Tipo.NORMAL));
                }
            }
        }

        for (int deltaColuna : new int[] {-1, 1}) {
            Posicao alvo = origem.deslocar(direcao, deltaColuna);
            if (!alvo.isValida()) continue;

            Peca noAlvo = tabuleiro.pecaEm(alvo);
            if (noAlvo != null && ehInimiga(noAlvo)) {
                adicionar(movimentos, origem, alvo); 
            } else if (noAlvo == null && alvo.equals(tabuleiro.getAlvoEnPassant())) {
                Peca peaoCapturado = tabuleiro.pecaEm(alvo.deslocar(-direcao, 0));
                if (peaoCapturado instanceof Peao && ehInimiga(peaoCapturado)) {
                    movimentos.add(new Movimento(origem, alvo, Movimento.Tipo.EN_PASSANT));
                }
            }
        }
        return movimentos;
    }

    private void adicionar(List<Movimento> movimentos, Posicao origem, Posicao destino) {
        boolean ultimaLinha = destino.getLinha() == 0 || destino.getLinha() == 7;
        if (ultimaLinha) {
            movimentos.add(new Movimento(origem, destino, Movimento.Tipo.PROMOCAO, new Rainha(getCor())));
            movimentos.add(new Movimento(origem, destino, Movimento.Tipo.PROMOCAO, new Torre(getCor())));
            movimentos.add(new Movimento(origem, destino, Movimento.Tipo.PROMOCAO, new Bispo(getCor())));
            movimentos.add(new Movimento(origem, destino, Movimento.Tipo.PROMOCAO, new Cavalo(getCor())));
        } else {
            movimentos.add(new Movimento(origem, destino, Movimento.Tipo.NORMAL));
        }
    }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♙" : "♟";
    }
}

class Torre extends PecaDeslizante {

    private static final int[][] DIRECOES = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

    public Torre(Cor cor) { super(cor); }

    @Override
    protected int[][] direcoes() { return DIRECOES; }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♖" : "♜";
    }
}

class Cavalo extends Peca {

    public static final int[][] DESLOCAMENTOS = {
        {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    public Cavalo(Cor cor) { super(cor); }

    @Override
    public List<Movimento> movimentosPseudoLegais(Tabuleiro tabuleiro, Posicao origem) {
        List<Movimento> movimentos = new ArrayList<>();
        for (int[] d : DESLOCAMENTOS) {
            Posicao alvo = origem.deslocar(d[0], d[1]);
            if (!alvo.isValida()) continue;
            Peca noAlvo = tabuleiro.pecaEm(alvo);
            if (noAlvo == null || ehInimiga(noAlvo)) {
                movimentos.add(new Movimento(origem, alvo, Movimento.Tipo.NORMAL));
            }
        }
        return movimentos;
    }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♘" : "♞";
    }
}

class Bispo extends PecaDeslizante {

    private static final int[][] DIRECOES = { {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };

    public Bispo(Cor cor) { super(cor); }

    @Override
    protected int[][] direcoes() { return DIRECOES; }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♗" : "♝";
    }
}

class Rainha extends PecaDeslizante {

    private static final int[][] DIRECOES = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},          { 0, 1},
        { 1, -1}, { 1, 0}, { 1, 1}
    };

    public Rainha(Cor cor) { super(cor); }

    @Override
    protected int[][] direcoes() { return DIRECOES; }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♕" : "♛";
    }
}

class Rei extends Peca {

    private static final int[][] DIRECOES = {
        {-1, -1}, {-1, 0}, {-1, 1},
        { 0, -1},          { 0, 1},
        { 1, -1}, { 1, 0}, { 1, 1}
    };

    public Rei(Cor cor) { super(cor); }

    @Override
    public List<Movimento> movimentosPseudoLegais(Tabuleiro tabuleiro, Posicao origem) {
        List<Movimento> movimentos = new ArrayList<>();

        for (int[] direcao : DIRECOES) {
            Posicao alvo = origem.deslocar(direcao[0], direcao[1]);
            if (!alvo.isValida()) continue;
            Peca noAlvo = tabuleiro.pecaEm(alvo);
            if (noAlvo == null || ehInimiga(noAlvo)) {
                movimentos.add(new Movimento(origem, alvo, Movimento.Tipo.NORMAL));
            }
        }

        if (!jaMoveu() && !tabuleiro.casaAtacada(origem, getCor().oposta())) {
            int linha = origem.getLinha();
            Cor inimiga = getCor().oposta();

            if (podeRoque(tabuleiro.pecaEm(new Posicao(linha, 7)))
                    && tabuleiro.pecaEm(new Posicao(linha, 5)) == null
                    && tabuleiro.pecaEm(new Posicao(linha, 6)) == null
                    && !tabuleiro.casaAtacada(new Posicao(linha, 5), inimiga)
                    && !tabuleiro.casaAtacada(new Posicao(linha, 6), inimiga)) {
                movimentos.add(new Movimento(origem, new Posicao(linha, 6), Movimento.Tipo.ROQUE_MENOR));
            }

            if (podeRoque(tabuleiro.pecaEm(new Posicao(linha, 0)))
                    && tabuleiro.pecaEm(new Posicao(linha, 1)) == null
                    && tabuleiro.pecaEm(new Posicao(linha, 2)) == null
                    && tabuleiro.pecaEm(new Posicao(linha, 3)) == null
                    && !tabuleiro.casaAtacada(new Posicao(linha, 3), inimiga)
                    && !tabuleiro.casaAtacada(new Posicao(linha, 2), inimiga)) {
                movimentos.add(new Movimento(origem, new Posicao(linha, 2), Movimento.Tipo.ROQUE_MAIOR));
            }
        }
        return movimentos;
    }

    private boolean podeRoque(Peca torre) {
        return torre instanceof Torre && torre.getCor() == getCor() && !torre.jaMoveu();
    }

    @Override
    public String getSimbolo() {
        return getCor() == Cor.BRANCA ? "♔" : "♚";
    }
}

class Tabuleiro {

    private static final int[][] DIRECOES_RETAS     = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
    private static final int[][] DIRECOES_DIAGONAIS = { {-1, -1}, {-1, 1}, {1, -1}, {1, 1} };

    private final Peca[][] grade = new Peca[8][8];
    private Posicao alvoEnPassant; 

    public Tabuleiro() {
        montarPosicaoInicial();
    }

    private void montarPosicaoInicial() {
        for (int coluna = 0; coluna < 8; coluna++) {
            grade[1][coluna] = new Peao(Cor.PRETA);
            grade[6][coluna] = new Peao(Cor.BRANCA);
        }
        grade[0][0] = new Torre(Cor.PRETA);   grade[0][7] = new Torre(Cor.PRETA);
        grade[0][1] = new Cavalo(Cor.PRETA);  grade[0][6] = new Cavalo(Cor.PRETA);
        grade[0][2] = new Bispo(Cor.PRETA);   grade[0][5] = new Bispo(Cor.PRETA);
        grade[0][3] = new Rainha(Cor.PRETA);  grade[0][4] = new Rei(Cor.PRETA);
        grade[7][0] = new Torre(Cor.BRANCA);  grade[7][7] = new Torre(Cor.BRANCA);
        grade[7][1] = new Cavalo(Cor.BRANCA); grade[7][6] = new Cavalo(Cor.BRANCA);
        grade[7][2] = new Bispo(Cor.BRANCA);  grade[7][5] = new Bispo(Cor.BRANCA);
        grade[7][3] = new Rainha(Cor.BRANCA); grade[7][4] = new Rei(Cor.BRANCA);
    }

    public Peca pecaEm(Posicao posicao) {
        if (posicao == null || !posicao.isValida()) return null;
        return grade[posicao.getLinha()][posicao.getColuna()];
    }

    public Posicao getAlvoEnPassant() { return alvoEnPassant; }

    public Posicao posicaoDoRei(Cor cor) {
        for (int linha = 0; linha < 8; linha++)
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = grade[linha][coluna];
                if (peca instanceof Rei && peca.getCor() == cor)
                    return new Posicao(linha, coluna);
            }
        return null;
    }
    public boolean casaAtacada(Posicao alvo, Cor atacante) {
        int linhaPeao = atacante == Cor.BRANCA ? alvo.getLinha() + 1 : alvo.getLinha() - 1;
        for (int dc : new int[] {-1, 1}) {
            Peca peca = pecaEm(new Posicao(linhaPeao, alvo.getColuna() + dc));
            if (peca instanceof Peao && peca.getCor() == atacante) return true;
        }
        for (int[] d : Cavalo.DESLOCAMENTOS) {
            Peca peca = pecaEm(alvo.deslocar(d[0], d[1]));
            if (peca instanceof Cavalo && peca.getCor() == atacante) return true;
        }
        for (int[][] direcoes : new int[][][] { DIRECOES_RETAS, DIRECOES_DIAGONAIS })
            for (int[] d : direcoes) {
                Peca peca = pecaEm(alvo.deslocar(d[0], d[1]));
                if (peca instanceof Rei && peca.getCor() == atacante) return true;
            }
        for (int[][] direcoes : new int[][][] { DIRECOES_RETAS, DIRECOES_DIAGONAIS })
            for (int[] d : direcoes) {
                Posicao atual = alvo.deslocar(d[0], d[1]);
                while (atual.isValida()) {
                    Peca peca = pecaEm(atual);
                    if (peca != null) {
                        if (peca.getCor() == atacante && (peca instanceof Rainha
                                || (peca instanceof Torre && ehReta(d))
                                || (peca instanceof Bispo && !ehReta(d)))) {
                            return true;
                        }
                        break; 
                    }
                    atual = atual.deslocar(d[0], d[1]);
                }
            }
        return false;
    }

    private boolean ehReta(int[] d) { return d[0] == 0 || d[1] == 0; }

    public void executarMovimento(Movimento m) {
        Posicao origem = m.getOrigem();
        Posicao destino = m.getDestino();
        Peca peca = grade[origem.getLinha()][origem.getColuna()];

        m.setPecaMovida(peca);
        m.setPecaJaMoveuAntes(peca.jaMoveu());
        m.setAlvoEnPassantAnterior(alvoEnPassant);
        m.setPecaCapturada(null);
        m.setPosicaoCapturada(null);

        alvoEnPassant = null; 

        switch (m.getTipo()) {
            case ROQUE_MENOR:
            case ROQUE_MAIOR: {
                boolean menor = m.getTipo() == Movimento.Tipo.ROQUE_MENOR;
                Posicao torreOrigem  = new Posicao(origem.getLinha(), menor ? 7 : 0);
                Posicao torreDestino = new Posicao(origem.getLinha(), menor ? 5 : 3);
                Peca torre = grade[torreOrigem.getLinha()][torreOrigem.getColuna()];

                m.setTorreMovida(torre);
                m.setTorreJaMoveuAntes(torre.jaMoveu());

                grade[destino.getLinha()][destino.getColuna()] = peca;
                grade[origem.getLinha()][origem.getColuna()] = null;
                grade[torreDestino.getLinha()][torreDestino.getColuna()] = torre;
                grade[torreOrigem.getLinha()][torreOrigem.getColuna()] = null;
                torre.setJaMoveu(true);
                break;
            }
            case EN_PASSANT: {
                int direcao = peca.getCor() == Cor.BRANCA ? -1 : 1;
                Posicao posPeaoCapturado = destino.deslocar(-direcao, 0);
                m.setPecaCapturada(grade[posPeaoCapturado.getLinha()][posPeaoCapturado.getColuna()]);
                m.setPosicaoCapturada(posPeaoCapturado);

                grade[posPeaoCapturado.getLinha()][posPeaoCapturado.getColuna()] = null;
                grade[destino.getLinha()][destino.getColuna()] = peca;
                grade[origem.getLinha()][origem.getColuna()] = null;
                break;
            }
            default: { 
                Peca capturada = grade[destino.getLinha()][destino.getColuna()];
                if (capturada != null) {
                    m.setPecaCapturada(capturada);
                    m.setPosicaoCapturada(destino);
                }
                Peca pecaFinal = m.getTipo() == Movimento.Tipo.PROMOCAO ? m.getPecaPromocao() : peca;
                grade[destino.getLinha()][destino.getColuna()] = pecaFinal;
                grade[origem.getLinha()][origem.getColuna()] = null;
                break;
            }
        }
        peca.setJaMoveu(true);

        if (peca instanceof Peao && Math.abs(destino.getLinha() - origem.getLinha()) == 2) {
            int linhaMedia = (origem.getLinha() + destino.getLinha()) / 2;
            alvoEnPassant = new Posicao(linhaMedia, origem.getColuna());
        }
    }

    public void desfazerMovimento(Movimento m) {
        Posicao origem = m.getOrigem();
        Posicao destino = m.getDestino();
        Peca peca = m.getPecaMovida();

        alvoEnPassant = m.getAlvoEnPassantAnterior();

        switch (m.getTipo()) {
            case ROQUE_MENOR:
            case ROQUE_MAIOR: {
                boolean menor = m.getTipo() == Movimento.Tipo.ROQUE_MENOR;
                Posicao torreOrigem  = new Posicao(origem.getLinha(), menor ? 7 : 0);
                Posicao torreDestino = new Posicao(origem.getLinha(), menor ? 5 : 3);
                Peca torre = m.getTorreMovida();

                grade[origem.getLinha()][origem.getColuna()] = peca;
                grade[destino.getLinha()][destino.getColuna()] = null;
                grade[torreOrigem.getLinha()][torreOrigem.getColuna()] = torre;
                grade[torreDestino.getLinha()][torreDestino.getColuna()] = null;
                torre.setJaMoveu(m.isTorreJaMoveuAntes());
                break;
            }
            case EN_PASSANT: {
                Posicao posPeao = m.getPosicaoCapturada();
                grade[origem.getLinha()][origem.getColuna()] = peca;
                grade[destino.getLinha()][destino.getColuna()] = null;
                grade[posPeao.getLinha()][posPeao.getColuna()] = m.getPecaCapturada();
                break;
            }
            default: { 
                grade[origem.getLinha()][origem.getColuna()] = peca;
                grade[destino.getLinha()][destino.getColuna()] = m.getPecaCapturada();
                break;
            }
        }
        peca.setJaMoveu(m.isPecaJaMoveuAntes());
    }
}

class Jogo {

    public enum Estado { EM_ANDAMENTO, XEQUE, XEQUE_MATE, EMPATE }

    private final Tabuleiro tabuleiro;
    private final List<Movimento> historico;
    private Cor turno;
    private Estado estado;

    public Jogo() {
        this.tabuleiro = new Tabuleiro();
        this.historico = new ArrayList<>();
        this.turno = Cor.BRANCA;
        this.estado = Estado.EM_ANDAMENTO;
    }

    public Tabuleiro getTabuleiro() { return tabuleiro; }
    public Cor getTurno()           { return turno; }
    public Estado getEstado()       { return estado; }

    public boolean acabou() { return estado == Estado.XEQUE_MATE || estado == Estado.EMPATE; }
    public boolean podeDesfazer() { return !historico.isEmpty(); }

    public Movimento getUltimoMovimento() {
        return historico.isEmpty() ? null : historico.get(historico.size() - 1);
    }

    public boolean estaEmXeque(Cor cor) {
        Posicao rei = tabuleiro.posicaoDoRei(cor);
        return rei != null && tabuleiro.casaAtacada(rei, cor.oposta());
    }

    public List<Movimento> movimentosLegais(Posicao origem) {
        List<Movimento> legais = new ArrayList<>();
        Peca peca = tabuleiro.pecaEm(origem);
        if (peca == null || peca.getCor() != turno) return legais;

        for (Movimento movimento : peca.movimentosPseudoLegais(tabuleiro, origem)) {
            if (!deixaReiEmXeque(movimento, peca.getCor())) {
                legais.add(movimento);
            }
        }
        return legais;
    }

    public void executarMovimento(Movimento movimento) {
        tabuleiro.executarMovimento(movimento);
        historico.add(movimento);
        turno = turno.oposta();
        atualizarEstado();
    }

    public void desfazerUltimoMovimento() {
        if (historico.isEmpty()) return;
        Movimento ultimo = historico.remove(historico.size() - 1);
        tabuleiro.desfazerMovimento(ultimo);
        turno = turno.oposta();
        atualizarEstado();
    }

    public List<Peca> pecasCapturadas(Cor cor) {
        List<Peca> capturadas = new ArrayList<>();
        for (Movimento movimento : historico) {
            Peca capturada = movimento.getPecaCapturada();
            if (capturada != null && capturada.getCor() == cor) {
                capturadas.add(capturada);
            }
        }
        return capturadas;
    }

    private boolean deixaReiEmXeque(Movimento movimento, Cor corQueMove) {
        tabuleiro.executarMovimento(movimento);
        boolean emXeque = estaEmXeque(corQueMove);
        tabuleiro.desfazerMovimento(movimento);
        return emXeque;
    }

    private boolean existeAlgumLanceLegal(Cor cor) {
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Peca peca = tabuleiro.pecaEm(new Posicao(linha, coluna));
                if (peca == null || peca.getCor() != cor) continue;

                Posicao origem = new Posicao(linha, coluna);
                for (Movimento movimento : peca.movimentosPseudoLegais(tabuleiro, origem)) {
                    if (!deixaReiEmXeque(movimento, peca.getCor())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void atualizarEstado() {
        boolean emXeque = estaEmXeque(turno);
        boolean temLance = existeAlgumLanceLegal(turno);

        if (emXeque && !temLance)      estado = Estado.XEQUE_MATE;
        else if (!emXeque && !temLance) estado = Estado.EMPATE;
        else if (emXeque)               estado = Estado.XEQUE;
        else                            estado = Estado.EM_ANDAMENTO;
    }
}

public class XadrezGUI extends JFrame {

    private static final Color CASA_CLARA  = new Color(240, 217, 181);
    private static final Color CASA_ESCURA = new Color(181, 136, 99);
    private static final Color DESTAQUE_SELECAO     = new Color(249, 238, 82);
    private static final Color DESTAQUE_MOVIMENTO   = new Color(123, 181, 92);
    private static final Color DESTAQUE_ULTIMO_LANCE = new Color(244, 201, 111);
    private static final Color DESTAQUE_XEQUE       = new Color(209, 89, 71);
    private final JButton[][] casas = new JButton[8][8];
    private final JLabel labelStatus = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel labelCapturasBrancas = new JLabel(" ");
    private final JLabel labelCapturasPretas = new JLabel(" ");
    private Jogo jogo;
    private Posicao selecao; 

    public XadrezGUI() {
        super("Xadrez — Java Swing");
        this.jogo = new Jogo();
        montarLayout();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        atualizarInterface();
    }

    private void montarLayout() {
        labelStatus.setFont(labelStatus.getFont().deriveFont(Font.BOLD, 18f));
        labelStatus.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(labelStatus, BorderLayout.NORTH);

        JPanel painelTabuleiro = new JPanel(new GridLayout(8, 8, 2, 2));
        painelTabuleiro.setPreferredSize(new Dimension(560, 560));
        painelTabuleiro.setBackground(Color.DARK_GRAY);
        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                JButton casa = new JButton();
                casa.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 38));
                casa.setFocusPainted(false);
                casa.setBorderPainted(false);
                casa.addActionListener(evento -> aoClicar(posicao));
                casa.setToolTipText(posicao.toString()); // ex.: "e4"
                casas[linha][coluna] = casa;
                painelTabuleiro.add(casa);
            }
        }
        JPanel centro = new JPanel(new GridBagLayout());
        centro.add(painelTabuleiro);
        add(centro, BorderLayout.CENTER);

        JPanel painelSul = new JPanel(new BorderLayout(8, 8));
        painelSul.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));

        JPanel painelCapturas = new JPanel(new GridLayout(2, 1, 4, 4));
        labelCapturasBrancas.setFont(labelCapturasBrancas.getFont().deriveFont(Font.PLAIN, 18f));
        labelCapturasPretas.setFont(labelCapturasPretas.getFont().deriveFont(Font.PLAIN, 18f));
        painelCapturas.add(labelCapturasBrancas);
        painelCapturas.add(labelCapturasPretas);
        painelSul.add(painelCapturas, BorderLayout.CENTER);

        JPanel painelBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botaoNovo = new JButton("Novo jogo");
        botaoNovo.addActionListener(evento -> reiniciar());
        JButton botaoDesfazer = new JButton("Desfazer lance");
        botaoDesfazer.addActionListener(evento -> desfazer());
        painelBotoes.add(botaoNovo);
        painelBotoes.add(botaoDesfazer);
        painelSul.add(painelBotoes, BorderLayout.SOUTH);

        add(painelSul, BorderLayout.SOUTH);
        pack();
    }

    private void aoClicar(Posicao posicao) {
        if (jogo.acabou()) return;

        if (selecao != null && selecao.equals(posicao)) {
            selecao = null; 
        } else if (selecao != null) {
            List<Movimento> opcoes = lancesPara(posicao);
            if (!opcoes.isEmpty()) {
                Movimento escolhido = opcoes.size() == 1 ? opcoes.get(0) : perguntarPromocao(opcoes);
                jogo.executarMovimento(escolhido);
                selecao = null;
                if (jogo.acabou()) anunciarFimDeJogo();
            } else {
                tentarSelecionar(posicao); 
            }
        } else {
            tentarSelecionar(posicao);
        }
        atualizarInterface();
    }

    private void tentarSelecionar(Posicao posicao) {
        Peca peca = jogo.getTabuleiro().pecaEm(posicao);
        selecao = (peca != null && peca.getCor() == jogo.getTurno()) ? posicao : null;
    }

    private List<Movimento> lancesPara(Posicao destino) {
        List<Movimento> opcoes = new ArrayList<>();
        if (selecao == null) return opcoes;
        for (Movimento movimento : jogo.movimentosLegais(selecao)) {
            if (movimento.getDestino().equals(destino)) opcoes.add(movimento);
        }
        return opcoes;
    }

    private Movimento perguntarPromocao(List<Movimento> opcoes) {
        String[] nomes = {"Rainha", "Torre", "Bispo", "Cavalo"};
        int escolha = JOptionPane.showOptionDialog(this,
                "O peão chegou à última casa. Escolha a peça da promoção:",
                "Promoção", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, nomes, nomes[0]);
        String nome = escolha >= 0 ? nomes[escolha] : "Rainha";
        for (Movimento movimento : opcoes) {
            if (movimento.getPecaPromocao().getClass().getSimpleName().equals(nome)) {
                return movimento;
            }
        }
        return opcoes.get(0);
    }

    private void anunciarFimDeJogo() {
        if (jogo.getEstado() == Jogo.Estado.XEQUE_MATE) {
            String vencedor = jogo.getTurno() == Cor.BRANCA ? "Pretas" : "Brancas";
            JOptionPane.showMessageDialog(this, "Xeque-mate! As " + vencedor + " venceram.",
                    "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Empate por afogamento: o rei não tem lances legais, mas não está em xeque.",
                    "Fim de jogo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void reiniciar() {
        jogo = new Jogo();
        selecao = null;
        atualizarInterface();
    }

    private void desfazer() {
        if (!jogo.podeDesfazer()) return;
        jogo.desfazerUltimoMovimento();
        selecao = null;
        atualizarInterface();
    }

    private void atualizarInterface() {
        Tabuleiro tabuleiro = jogo.getTabuleiro();
        Movimento ultimoLance = jogo.getUltimoMovimento();

        Posicao reiEmXeque = null;
        if (jogo.getEstado() == Jogo.Estado.XEQUE || jogo.getEstado() == Jogo.Estado.XEQUE_MATE) {
            reiEmXeque = tabuleiro.posicaoDoRei(jogo.getTurno());
        }

        List<Movimento> lancesDaSelecao = new ArrayList<>();
        if (selecao != null) lancesDaSelecao.addAll(jogo.movimentosLegais(selecao));

        for (int linha = 0; linha < 8; linha++) {
            for (int coluna = 0; coluna < 8; coluna++) {
                Posicao posicao = new Posicao(linha, coluna);
                Peca peca = tabuleiro.pecaEm(posicao);
                JButton casa = casas[linha][coluna];

                casa.setText(peca == null ? "" : peca.getSimbolo());
                casa.setForeground(peca != null && peca.getCor() == Cor.BRANCA
                        ? new Color(50, 50, 50) : Color.BLACK);
                casa.setBackground(corDaCasa(posicao, linha, coluna, ultimoLance, reiEmXeque, lancesDaSelecao));
            }
        }

        labelStatus.setText(textoDeStatus());
        labelCapturasBrancas.setText("Brancas capturaram: " + simbolosCapturados(Cor.PRETA));
        labelCapturasPretas.setText("Pretas capturaram: " + simbolosCapturados(Cor.BRANCA));
    }

    private Color corDaCasa(Posicao posicao, int linha, int coluna, Movimento ultimoLance,
                            Posicao reiEmXeque, List<Movimento> lancesDaSelecao) {
        Color cor = (linha + coluna) % 2 == 0 ? CASA_CLARA : CASA_ESCURA;
        if (ultimoLance != null && (posicao.equals(ultimoLance.getOrigem())
                || posicao.equals(ultimoLance.getDestino()))) {
            cor = DESTAQUE_ULTIMO_LANCE;
        }
        if (posicao.equals(reiEmXeque)) cor = DESTAQUE_XEQUE;
        if (posicao.equals(selecao))    cor = DESTAQUE_SELECAO;
        for (Movimento lance : lancesDaSelecao) {
            if (lance.getDestino().equals(posicao)) { cor = DESTAQUE_MOVIMENTO; break; }
        }
        return cor;
    }

    private String textoDeStatus() {
        String vez = jogo.getTurno() == Cor.BRANCA ? "Brancas" : "Pretas";
        switch (jogo.getEstado()) {
            case XEQUE_MATE: return "Xeque-mate! Vitória das "
                    + (jogo.getTurno() == Cor.BRANCA ? "Pretas" : "Brancas");
            case EMPATE:     return "Empate por afogamento";
            case XEQUE:      return "Xeque! Vez das " + vez;
            default:         return "Vez das " + vez;
        }
    }

    private String simbolosCapturados(Cor cor) {
        StringBuilder simbolos = new StringBuilder();
        for (Peca peca : jogo.pecasCapturadas(cor)) {
            simbolos.append(peca.getSimbolo()).append(' ');
        }
        return simbolos.length() == 0 ? "—" : simbolos.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new XadrezGUI().setVisible(true));
    }
}
