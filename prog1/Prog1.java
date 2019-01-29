import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.Scanner;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;

public class Prog1 {
    static int N;
    static int D;
    static int K;
    public static void main (String[] args) {
        int action = parseArgs(args);
        switch (action) {
            case 1:
            case 2:
                trainMode(args, action);
                break;
            case 3:
                predictMode(args);
                break;
            case 4:
                evaluateMode(args);
                break;
            default:
                usage();
        }
    }

    private static int parseArgs(String[] args) {
        if (args.length >= 1) {
            try {
                switch (args[0]) {
                    case "-train":
                        if (args.length == 8 && args[4].equals("a")) {
                            N = Integer.parseInt(args[5]);
                            D = Integer.parseInt(args[6]);
                            K = Integer.parseInt(args[7]);
                            return 1;
                        } else if (args.length == 10 && args[4].equals("g")) {
                            N = Integer.parseInt(args[7]);
                            D = Integer.parseInt(args[8]);
                            K = Integer.parseInt(args[9]);
                            return 2;
                        }
                        break;
                    case "-pred":
                        if (args.length == 7) {
                            N = Integer.parseInt(args[4]);
                            D = Integer.parseInt(args[5]);
                            K = Integer.parseInt(args[6]);
                            return 3;
                        }
                        break;
                    case "-eval":
                        if (args.length == 7) {
                            N = Integer.parseInt(args[4]);
                            D = Integer.parseInt(args[5]);
                            K = Integer.parseInt(args[6]);
                            return 4;
                        }
                        break;
                    default:
                        break;
                }
            } catch(NumberFormatException nfe) {
                System.out.println("Error converting arguments to integers");
                usage();
                System.exit(1);
            }
            
        }
        return 0;
    }

    // When called:
    // idx:  0      1     2     3         [4 5 6 7 | 4 5  6  7 8 9]
    // args: -train x.txt y.txt out.model [a N D K | g ss st N D K]
    private static void trainMode(String[] args, int mode) {
        if (mode == 1) {
            trainAnalytical(args);
        } else if (mode == 2) {
            trainGradDescent(args);
        }
    }

    private static void trainAnalytical(String[] args) {
        // x is N x D+1 matrix from given feature file
        RealMatrix x = loadMatrix(N, D+1, args[1], true);
        // y is D vector from given output file
        RealMatrix y = loadMatrix(N, 1, args[2], false);
        
        RealMatrix z = MatrixUtils.inverse(x.transpose().multiply(x));
        RealMatrix w = z.multiply(x.transpose()).multiply(y);
        writeMatrix(w, args[3]);
    }

    private static void trainGradDescent(String[] args) {

    }

    // When called:
    // idx:  0     1     2        3               4 5 6
    // args: -pred x.txt in.model out.predictions N D K
    private static void predictMode(String[] args) {
        // x is N x D+1 matrix from given feature file
        RealMatrix x = loadMatrix(N, D+1, args[1], true).transpose();
        // w is D+1 vector from given model file
        RealMatrix w = loadMatrix(1, D+1, args[2], false).transpose();

        RealMatrix p = predict(x, w);
        writeMatrix(p, args[3]);
    }

    private static RealMatrix predict(RealMatrix x, RealMatrix w) {
        return x.transpose().multiply(w);
    }

    // When called:
    // idx:  0     1     2     3        4 5 6
    // args: -eval x.txt y.txt in.model N D K
    private static void evaluateMode(String[] args) {
        // x is N x D+1 matrix from given feature file
        RealMatrix x = loadMatrix(N, D+1, args[1], true).transpose();
        // y is D vector from given output file
        RealMatrix y = loadMatrix(N, 1, args[2], false);
        // w is D+1 vector from given model file
        RealMatrix w = loadMatrix(1, D+1, args[3], false).transpose();

        RealMatrix error = predict(x, w).subtract(y);
        double mse = Math.pow(error.getFrobeniusNorm(), 2) / N;
        System.out.printf("%.3e\n", mse);
    }

    private static RealMatrix loadMatrix(int n, int d, String filepath, boolean featureMatrix) {
        try {
            File file = new File(filepath);
            Scanner fileScanner = new Scanner(file);
            Scanner lineScanner;
            // matrix is N x D dimensional
            double[][] matrix = new double[n][d];
            try {
                int row = 0;
                int col;
                while (fileScanner.hasNextLine()) {
                    lineScanner = new Scanner(fileScanner.nextLine());
                    if (featureMatrix) {
                        // prepend 1 to feature vector and set offset
                        matrix[row][0] = 1.0;
                        col = 1;
                    } else {
                        col = 0;
                    }
                    // copy feature row into rest of the matrix row
                    while (lineScanner.hasNextDouble()) {
                        matrix[row][col] = lineScanner.nextDouble();
                        col++;
                    }
                    row++;
                }
            } catch (ArrayIndexOutOfBoundsException oobe) {
                System.out.printf("Mismatched input matrix dimensions in file:\nExpected %d by %d\n", n, d);
                System.exit(1);
            }
            
            printMatrix(matrix);
            
            return MatrixUtils.createRealMatrix(matrix);
        } catch (FileNotFoundException fnfe) {
            System.out.printf("Could not find file %s\n", filepath);
            System.exit(1);
        }
        return null;
    }

    private static void printMatrix(double[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            System.out.print("[ ");
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println("]");
        }
    }

    private static void writeMatrix(RealMatrix m, String filepath) {
        try {
            File file = new File(filepath);
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);
            double[][] data = m.getData();
            for (double[] row : data) {
                for (double datum : row) {
                    bw.write(String.format("%.3e ", datum));
                }
                System.out.println();
            }
            bw.close();
        } catch (FileNotFoundException fnfe) {
            System.out.printf("Could not find file %s\n", filepath);
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println("Error while writing to file.");
            System.exit(1);
        }
    }

    private static void usage() {
        System.out.println("java Prog1 [-train x.txt y.txt out.model [a | g ss st]\n"
                                    +"\t| -pred x.txt in.model out.predictions\n"
                                    +"\t| -eval x.txt y.txt in.model ] N D K");
    }
}