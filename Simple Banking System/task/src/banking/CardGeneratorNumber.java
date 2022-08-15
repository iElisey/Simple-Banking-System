package banking;

import java.util.Random;

public class CardGeneratorNumber {
    private static final Random RNG = new Random(System.currentTimeMillis());

    private static final int CARD_NUMBER_LENGTH = 16;
    private static final int DIGIT_UPPER_LIMIT = 10;

    public String generate(String bankIdNumber) {
        int randomNumberLength = CARD_NUMBER_LENGTH - (bankIdNumber.length() + 1);

        StringBuilder builder = new StringBuilder(bankIdNumber);

        for (int i = 0; i < randomNumberLength; i++)
            builder.append(RNG.nextInt(DIGIT_UPPER_LIMIT));
        builder.append(getCheckDigit(builder.toString()));

        return builder.toString();
    }

    public String generate() {
        return generate("");
    }

    public static boolean checkLuhn(String cardNo)
    {
        int nDigits = cardNo.length();

        int nSum = 0;
        boolean isSecond = false;
        for (int i = nDigits - 1; i >= 0; i--)
        {

            int d = cardNo.charAt(i) - '0';

            if (isSecond == true)
                d = d * 2;

            // We add two digits to handle
            // cases that make two digits
            // after doubling
            nSum += d / 10;
            nSum += d % 10;

            isSecond = !isSecond;
        }
        return (nSum % 10 == 0);
    }

    private int getCheckDigit(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = Integer.parseInt(number.substring(i, (i + 1)));

            if ((i % 2) == 0) {
                digit = digit * 2;

                if (digit > 9)
                    digit = (digit / 10) + (digit % 10);
            }
            sum += digit;
        }

        int mod = sum % 10;

        return ((mod == 0) ? 0 : 10 - mod);
    }
}
